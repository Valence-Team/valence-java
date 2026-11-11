package br.com.valence.api.services;

import br.com.valence.api.dtos.FonteDto;
import br.com.valence.api.dtos.HipoteseDto;
import br.com.valence.api.dtos.eventos.OcorrenciaTriadaEventoDto;
import br.com.valence.api.dtos.ia.ClasseVisaoDto;
import br.com.valence.api.dtos.ia.RagResultadoDto;
import br.com.valence.api.dtos.ia.TriagemIaDto;
import br.com.valence.api.enums.Cultura;
import br.com.valence.api.enums.StatusOcorrencia;
import br.com.valence.api.mappers.ConversorJson;
import br.com.valence.api.models.FotoOcorrenciaModel;
import br.com.valence.api.models.OcorrenciaModel;
import br.com.valence.api.models.TriagemModel;
import br.com.valence.api.repositories.OcorrenciaRepository;
import br.com.valence.api.utils.DadosPessoaisUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class TriagemRegistroService {

    private final OcorrenciaRepository ocorrenciaRepository;
    private final ArmazenamentoFotoService armazenamentoFotoService;
    private final ConversorJson conversorJson;
    private final ApplicationEventPublisher eventPublisher;

    @Value("${valence.ia.confianca-minima}")
    private double confiancaMinima;

    @Transactional(readOnly = true)
    public Optional<DadosTriagem> carregarDados(Long ocorrenciaId) {
        return ocorrenciaRepository.findById(ocorrenciaId)
                .filter(ocorrencia -> ocorrencia.getStatus().permiteTriagem())
                .map(ocorrencia -> {
                    FotoOcorrenciaModel ultimaFoto = ocorrencia.getFotos().isEmpty()
                            ? null
                            : ocorrencia.getFotos().get(ocorrencia.getFotos().size() - 1);
                    FotoParaAnalise foto = ultimaFoto == null ? null : new FotoParaAnalise(
                            armazenamentoFotoService.ler(ultimaFoto.getCaminhoArquivo()),
                            "foto-" + ultimaFoto.getId(), ultimaFoto.getContentType());
                    return new DadosTriagem(ocorrencia.getId(), ocorrencia.getCultura(), ocorrencia.getDescricao(),
                            foto);
                });
    }

    @Transactional
    public void registrarFotoRecusada(Long ocorrenciaId, String motivo) {
        OcorrenciaModel ocorrencia = carregarParaTriagem(ocorrenciaId);
        if (ocorrencia == null) {
            return;
        }
        TriagemModel triagem = triagemDe(ocorrencia);
        triagem.setHipoteses(conversorJson.paraJson(List.of()));
        triagem.setConfianca(null);
        triagem.setSeveridade(null);
        triagem.setCuidadosImediatos(conversorJson.paraJson(List.of()));
        triagem.setTextoAlerta(null);
        triagem.setEncaminharAgronomo(true);
        triagem.setExplicacao(null);
        triagem.setFontes(conversorJson.paraJson(List.of()));
        triagem.setMotivoImagem(motivo == null ? "IMAGEM_INVALIDA" : DadosPessoaisUtils.limitar(motivo, 40));
        triagem.setGeradaEm(Instant.now());
        ocorrencia.setSeveridade(null);
        ocorrencia.setStatus(StatusOcorrencia.EM_ANALISE);
        ocorrencia.setAtualizadoEm(Instant.now());
    }

    @Transactional
    public void registrar(Long ocorrenciaId, Optional<TriagemIaDto> triagemIa, List<ClasseVisaoDto> classes,
                          RagResultadoDto rag, Double confianca) {
        OcorrenciaModel ocorrencia = carregarParaTriagem(ocorrenciaId);
        if (ocorrencia == null) {
            return;
        }
        boolean geradaPeloModelo = triagemIa.isPresent();
        boolean confiavel = geradaPeloModelo && confianca != null && confianca >= confiancaMinima;

        TriagemModel triagem = triagemDe(ocorrencia);
        List<HipoteseDto> hipoteses = triagemIa.map(TriagemIaDto::hipoteses)
                .orElseGet(() -> classes.stream()
                        .map(classe -> new HipoteseDto(classe.nome() != null ? classe.nome() : classe.rotulo(),
                                classe.confianca()))
                        .toList());
        triagem.setHipoteses(conversorJson.paraJson(hipoteses));
        triagem.setConfianca(confianca);
        triagem.setSeveridade(triagemIa.map(TriagemIaDto::severidade).orElse(null));
        triagem.setCuidadosImediatos(conversorJson.paraJson(
                triagemIa.map(TriagemIaDto::cuidadosImediatos).orElse(List.of())));
        triagem.setTextoAlerta(triagemIa.map(TriagemIaDto::textoAlerta).orElse(null));
        triagem.setEncaminharAgronomo(!confiavel || triagemIa.get().encaminharAgronomo());
        triagem.setExplicacao(rag == null ? null : DadosPessoaisUtils.limitar(rag.explicacao(), 1500));
        triagem.setFontes(conversorJson.paraJson(rag == null ? List.of() : rag.trechosOuVazio().stream()
                .map(trecho -> new FonteDto(trecho.titulo(), trecho.fonte(), trecho.url()))
                .toList()));
        triagem.setMotivoImagem(null);
        triagem.setGeradaEm(Instant.now());

        ocorrencia.setSeveridade(triagem.getSeveridade());
        ocorrencia.setStatus(confiavel ? StatusOcorrencia.TRIADA : StatusOcorrencia.EM_ANALISE);
        ocorrencia.setAtualizadoEm(Instant.now());

        if (confiavel) {
            eventPublisher.publishEvent(new OcorrenciaTriadaEventoDto(UUID.randomUUID(),
                    OcorrenciaTriadaEventoDto.TIPO, 1, Instant.now(), ocorrencia.getId(), ocorrencia.getCultura(),
                    hipoteses.isEmpty() ? null : hipoteses.get(0).praga(), triagem.getSeveridade(), confianca,
                    ocorrencia.getPropriedade().getMunicipio(), ocorrencia.getPropriedade().getUf(),
                    ocorrencia.getDataRegistro()));
        }
        log.info("Ocorrência {} triada com status {}.", ocorrencia.getId(), ocorrencia.getStatus());
    }

    private OcorrenciaModel carregarParaTriagem(Long ocorrenciaId) {
        return ocorrenciaRepository.findById(ocorrenciaId)
                .filter(ocorrencia -> ocorrencia.getStatus().permiteTriagem())
                .orElse(null);
    }

    private TriagemModel triagemDe(OcorrenciaModel ocorrencia) {
        if (ocorrencia.getTriagem() == null) {
            TriagemModel triagem = new TriagemModel();
            triagem.setOcorrencia(ocorrencia);
            ocorrencia.setTriagem(triagem);
        }
        return ocorrencia.getTriagem();
    }

    public record FotoParaAnalise(byte[] conteudo, String nomeArquivo, String contentType) {
    }

    public record DadosTriagem(Long ocorrenciaId, Cultura cultura, String descricao, FotoParaAnalise foto) {
    }
}
