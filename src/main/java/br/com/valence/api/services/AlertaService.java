package br.com.valence.api.services;

import br.com.valence.api.dtos.AlertaResponseDto;
import br.com.valence.api.dtos.PaginaDto;
import br.com.valence.api.dtos.eventos.OcorrenciaTriadaEventoDto;
import br.com.valence.api.enums.DecisaoLaudo;
import br.com.valence.api.enums.NivelAlerta;
import br.com.valence.api.enums.StatusOcorrencia;
import br.com.valence.api.exceptions.RecursoNaoEncontradoException;
import br.com.valence.api.mappers.OcorrenciaMapper;
import br.com.valence.api.models.AlertaModel;
import br.com.valence.api.models.EventoProcessadoModel;
import br.com.valence.api.models.LaudoModel;
import br.com.valence.api.models.OcorrenciaModel;
import br.com.valence.api.models.PropriedadeModel;
import br.com.valence.api.models.UsuarioModel;
import br.com.valence.api.repositories.AlertaRepository;
import br.com.valence.api.repositories.EventoProcessadoRepository;
import br.com.valence.api.repositories.OcorrenciaRepository;
import br.com.valence.api.repositories.PropriedadeRepository;
import br.com.valence.api.utils.GeoUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Locale;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class AlertaService {

    private final AlertaRepository alertaRepository;
    private final PropriedadeRepository propriedadeRepository;
    private final OcorrenciaRepository ocorrenciaRepository;
    private final EventoProcessadoRepository eventoProcessadoRepository;
    private final OcorrenciaMapper ocorrenciaMapper;

    @Value("${valence.alerta.raio-km}")
    private double raioKm;

    @Transactional(readOnly = true)
    public PaginaDto<AlertaResponseDto> listar(UsuarioModel usuario, Pageable pageable) {
        return PaginaDto.de(alertaRepository.buscarDoUsuario(usuario.getId(), pageable).map(this::paraResposta));
    }

    @Transactional
    public AlertaResponseDto marcarComoLido(Long id, UsuarioModel usuario) {
        AlertaModel alerta = alertaRepository.buscarPorIdDoUsuario(id, usuario.getId())
                .orElseThrow(() -> new RecursoNaoEncontradoException("Alerta não encontrado."));
        alerta.setLido(true);
        return paraResposta(alerta);
    }

    @Transactional
    public int gerarAlertasDeSuspeita(OcorrenciaTriadaEventoDto evento) {
        if (evento == null || evento.eventoId() == null || evento.ocorrenciaId() == null) {
            log.warn("Evento ocorrencia.triada sem identificação descartado.");
            return 0;
        }
        if (eventoProcessadoRepository.existsById(evento.eventoId())) {
            log.info("Evento {} já processado; ignorado.", evento.eventoId());
            return 0;
        }
        int criados = ocorrenciaRepository.findById(evento.ocorrenciaId())
                .filter(ocorrencia -> ocorrencia.getStatus() == StatusOcorrencia.TRIADA)
                .map(ocorrencia -> criarAlertas(ocorrencia, NivelAlerta.SUSPEITA))
                .orElse(0);
        eventoProcessadoRepository.save(new EventoProcessadoModel(evento.eventoId(),
                OcorrenciaTriadaEventoDto.TIPO, Instant.now()));
        return criados;
    }

    @Transactional
    public void aplicarLaudo(OcorrenciaModel ocorrencia, DecisaoLaudo decisao) {
        if (decisao == DecisaoLaudo.CONFIRMADA) {
            alertaRepository.findByOcorrenciaId(ocorrencia.getId())
                    .forEach(alerta -> alerta.setNivel(NivelAlerta.CONFIRMADO));
            criarAlertas(ocorrencia, NivelAlerta.CONFIRMADO);
        } else if (decisao == DecisaoLaudo.DESCARTADA) {
            alertaRepository.findByOcorrenciaId(ocorrencia.getId())
                    .forEach(alerta -> alerta.setNivel(NivelAlerta.ENCERRADO));
        }
    }

    private int criarAlertas(OcorrenciaModel ocorrencia, NivelAlerta nivel) {
        PropriedadeModel origem = ocorrencia.getPropriedade();
        double deltaLatitude = GeoUtils.grausDeLatitude(raioKm);
        double deltaLongitude = GeoUtils.grausDeLongitude(raioKm, origem.getLatitude());
        int criados = 0;
        for (PropriedadeModel destino : propriedadeRepository.buscarCandidatasAlerta(ocorrencia.getCultura(),
                origem.getId(), origem.getLatitude() - deltaLatitude, origem.getLatitude() + deltaLatitude,
                origem.getLongitude() - deltaLongitude, origem.getLongitude() + deltaLongitude)) {
            double distancia = GeoUtils.distanciaKm(origem.getLatitude(), origem.getLongitude(),
                    destino.getLatitude(), destino.getLongitude());
            if (distancia > raioKm
                    || alertaRepository.existsByOcorrenciaIdAndPropriedadeId(ocorrencia.getId(), destino.getId())) {
                continue;
            }
            AlertaModel alerta = new AlertaModel();
            alerta.setNivel(nivel);
            alerta.setDistanciaKm((double) Math.max(1, Math.round(distancia)));
            alerta.setLido(false);
            alerta.setCriadoEm(Instant.now());
            alerta.setOcorrencia(ocorrencia);
            alerta.setPropriedade(destino);
            alertaRepository.save(alerta);
            criados++;
        }
        log.info("Ocorrência {}: {} alerta(s) {} criados.", ocorrencia.getId(), criados, nivel);
        return criados;
    }

    private AlertaResponseDto paraResposta(AlertaModel alerta) {
        OcorrenciaModel ocorrencia = alerta.getOcorrencia();
        String praga = ocorrenciaMapper.pragaProvavel(ocorrencia.getTriagem());
        if (alerta.getNivel() == NivelAlerta.CONFIRMADO) {
            praga = ocorrencia.getLaudos().stream()
                    .filter(laudo -> laudo.getDecisao() == DecisaoLaudo.CONFIRMADA)
                    .map(LaudoModel::getPragaConfirmada)
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(praga);
        }
        return new AlertaResponseDto(alerta.getId(), alerta.getNivel(), alerta.getDistanciaKm(), alerta.isLido(),
                alerta.getCriadoEm(), ocorrencia.getId(), ocorrencia.getCultura(), praga,
                textoDoAlerta(alerta, praga), ocorrencia.getSeveridade(), alerta.getPropriedade().getNome());
    }

    private String textoDoAlerta(AlertaModel alerta, String praga) {
        String cultura = alerta.getOcorrencia().getCultura().name().toLowerCase(Locale.ROOT);
        String distancia = String.format(Locale.ROOT, "%.0f", alerta.getDistanciaKm());
        String nomePraga = praga == null ? "praga" : praga;
        return switch (alerta.getNivel()) {
            case CONFIRMADO -> "Caso de " + nomePraga + " confirmado por agrônomo em lavoura de " + cultura
                    + " a cerca de " + distancia + " km. Observe suas plantas e registre qualquer sinal.";
            case ENCERRADO -> "Alerta encerrado: o agrônomo descartou a suspeita a cerca de " + distancia + " km.";
            case SUSPEITA -> {
                String texto = alerta.getOcorrencia().getTriagem() == null
                        ? null
                        : alerta.getOcorrencia().getTriagem().getTextoAlerta();
                yield texto != null ? texto : "Possível caso de " + nomePraga + " em lavoura de " + cultura
                        + " a cerca de " + distancia + " km. Observe suas plantas.";
            }
        };
    }
}
