package br.com.valence.api.schedulers;

import br.com.valence.api.enums.StatusOcorrencia;
import br.com.valence.api.models.OcorrenciaModel;
import br.com.valence.api.repositories.OcorrenciaRepository;
import br.com.valence.api.services.TriagemService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(name = "valence.triagem.agendador.habilitado", havingValue = "true", matchIfMissing = true)
public class TriagemPendenteScheduler {

    private final OcorrenciaRepository ocorrenciaRepository;
    private final TriagemService triagemService;

    @Value("${valence.triagem.espera-foto-minutos:10}")
    private long esperaFotoMinutos;

    @Scheduled(initialDelay = 30000, fixedDelayString = "${valence.triagem.agendador.intervalo-ms:60000}")
    public void triarPendentes() {
        Instant limite = Instant.now().minus(esperaFotoMinutos, ChronoUnit.MINUTES);
        for (OcorrenciaModel ocorrencia : ocorrenciaRepository
                .findTop5ByStatusAndDataRegistroBeforeOrderByDataRegistroAsc(StatusOcorrencia.RECEBIDA, limite)) {
            try {
                triagemService.triar(ocorrencia.getId());
            } catch (RuntimeException ex) {
                log.warn("Não foi possível triar a ocorrência pendente {}.", ocorrencia.getId());
            }
        }
    }
}
