package br.com.valence.api.services;

import br.com.valence.api.dtos.LaudoDto;
import br.com.valence.api.dtos.LaudoResponseDto;
import br.com.valence.api.dtos.eventos.LaudoEmitidoEventoDto;
import br.com.valence.api.enums.DecisaoLaudo;
import br.com.valence.api.enums.StatusOcorrencia;
import br.com.valence.api.exceptions.RegraNegocioException;
import br.com.valence.api.exceptions.ValidacaoException;
import br.com.valence.api.models.LaudoModel;
import br.com.valence.api.models.OcorrenciaModel;
import br.com.valence.api.models.UsuarioModel;
import br.com.valence.api.repositories.LaudoRepository;
import br.com.valence.api.repositories.UsuarioRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class LaudoService {

    private final OcorrenciaService ocorrenciaService;
    private final LaudoRepository laudoRepository;
    private final UsuarioRepository usuarioRepository;
    private final AlertaService alertaService;
    private final MuralService muralService;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public LaudoResponseDto emitir(Long ocorrenciaId, LaudoDto dto, UsuarioModel agronomo) {
        OcorrenciaModel ocorrencia = ocorrenciaService.carregar(ocorrenciaId);
        if (ocorrencia.getStatus().finalizada()) {
            throw new RegraNegocioException("A ocorrência já tem laudo final.");
        }
        if (dto.getDecisao() == DecisaoLaudo.CONFIRMADA && !StringUtils.hasText(dto.getPragaConfirmada())) {
            throw new ValidacaoException("pragaConfirmada", "Informe a praga confirmada.");
        }
        Instant agora = Instant.now();
        LaudoModel laudo = new LaudoModel();
        laudo.setDecisao(dto.getDecisao());
        laudo.setPragaConfirmada(StringUtils.hasText(dto.getPragaConfirmada()) ? dto.getPragaConfirmada().trim() : null);
        laudo.setParecer(dto.getParecer().trim());
        laudo.setEmitidoEm(agora);
        laudo.setOcorrencia(ocorrencia);
        laudo.setAgronomo(usuarioRepository.getReferenceById(agronomo.getId()));
        laudoRepository.save(laudo);
        ocorrencia.getLaudos().add(laudo);

        ocorrencia.setStatus(switch (dto.getDecisao()) {
            case CONFIRMADA -> StatusOcorrencia.CONFIRMADA;
            case DESCARTADA -> StatusOcorrencia.DESCARTADA;
            case MAIS_INFORMACOES -> StatusOcorrencia.EM_ANALISE;
        });
        ocorrencia.setAtualizadoEm(agora);
        alertaService.aplicarLaudo(ocorrencia, dto.getDecisao());
        if (dto.getDecisao() == DecisaoLaudo.CONFIRMADA) {
            muralService.publicarCasoConfirmado(ocorrencia, laudo, agronomo);
        }
        eventPublisher.publishEvent(new LaudoEmitidoEventoDto(UUID.randomUUID(), LaudoEmitidoEventoDto.TIPO, 1, agora,
                ocorrencia.getId(), laudo.getDecisao(), laudo.getPragaConfirmada(), ocorrencia.getCultura(),
                ocorrencia.getPropriedade().getMunicipio(), ocorrencia.getPropriedade().getUf()));
        return new LaudoResponseDto(laudo.getId(), laudo.getDecisao(), laudo.getPragaConfirmada(), laudo.getParecer(),
                laudo.getEmitidoEm(), agronomo.getNome());
    }
}
