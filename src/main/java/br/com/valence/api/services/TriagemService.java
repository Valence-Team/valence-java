package br.com.valence.api.services;

import br.com.valence.api.clients.IaServiceClient;
import br.com.valence.api.dtos.HipoteseDto;
import br.com.valence.api.dtos.ia.ClasseVisaoDto;
import br.com.valence.api.dtos.ia.EntradaTriagemDto;
import br.com.valence.api.dtos.ia.RagResultadoDto;
import br.com.valence.api.dtos.ia.TriagemIaDto;
import br.com.valence.api.dtos.ia.VisaoResultadoDto;
import br.com.valence.api.exceptions.IaIndisponivelException;
import br.com.valence.api.utils.DadosPessoaisUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class TriagemService {

    private final TriagemRegistroService triagemRegistroService;
    private final IaServiceClient iaServiceClient;
    private final TriagemIaService triagemIaService;

    public void triar(Long ocorrenciaId) {
        Optional<TriagemRegistroService.DadosTriagem> carregados = triagemRegistroService.carregarDados(ocorrenciaId);
        if (carregados.isEmpty()) {
            return;
        }
        TriagemRegistroService.DadosTriagem dados = carregados.get();

        // Visão computacional
        boolean servicoIaDisponivel = true;
        VisaoResultadoDto visao = null;
        if (dados.foto() != null) {
            try {
                visao = iaServiceClient.classificar(dados.foto().conteudo(), dados.foto().nomeArquivo(),
                        dados.foto().contentType(), dados.cultura());
            } catch (IaIndisponivelException ex) {
                servicoIaDisponivel = false;
                log.warn("Ocorrência {}: serviço de IA indisponível; triagem segue só com o texto.", ocorrenciaId);
            }
        }
        if (visao != null && !visao.imagemValida()) {
            triagemRegistroService.registrarFotoRecusada(ocorrenciaId, visao.motivo());
            return;
        }
        List<ClasseVisaoDto> classes = visao == null ? List.of() : visao.classesOuVazio().stream()
                .filter(classe -> classe.confianca() != null)
                .sorted(Comparator.comparing(ClasseVisaoDto::confianca).reversed())
                .limit(3)
                .toList();

        // RAG sobre as fichas técnicas
        RagResultadoDto rag = null;
        if (servicoIaDisponivel) {
            try {
                rag = iaServiceClient.consultarRag(dados.cultura(),
                        DadosPessoaisUtils.mascarar(DadosPessoaisUtils.limitar(dados.descricao(), 1000)),
                        classes.stream().map(ClasseVisaoDto::rotulo).toList());
            } catch (IaIndisponivelException ex) {
                log.warn("Ocorrência {}: consulta às fichas técnicas indisponível.", ocorrenciaId);
            }
        }

        // Spring AI: triagem estruturada
        Optional<TriagemIaDto> triagemIa = triagemIaService.gerar(new EntradaTriagemDto(dados.cultura(),
                dados.descricao(), classes, rag == null ? List.of() : rag.trechosOuVazio()));

        Double confianca = !classes.isEmpty()
                ? classes.get(0).confianca()
                : triagemIa.flatMap(triagem -> triagem.hipoteses().stream().findFirst())
                        .map(HipoteseDto::probabilidade)
                        .orElse(null);
        triagemRegistroService.registrar(ocorrenciaId, triagemIa, classes, rag, confianca);
    }
}
