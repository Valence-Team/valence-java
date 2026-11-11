package br.com.valence.api.producers;

import br.com.valence.api.dtos.eventos.LaudoEmitidoEventoDto;
import br.com.valence.api.dtos.eventos.OcorrenciaTriadaEventoDto;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.UUID;

@Slf4j
@Component
@RequiredArgsConstructor
public class EventoProducer {

    private final RabbitTemplate rabbitTemplate;

    @Value("${valence.rabbitmq.exchange}")
    private String exchange;

    @Value("${valence.rabbitmq.rk-ocorrencia-triada}")
    private String routingKeyOcorrenciaTriada;

    @Value("${valence.rabbitmq.rk-laudo-emitido}")
    private String routingKeyLaudoEmitido;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void publicarOcorrenciaTriada(OcorrenciaTriadaEventoDto evento) {
        publicar(routingKeyOcorrenciaTriada, evento.eventoId(), evento);
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT, fallbackExecution = true)
    public void publicarLaudoEmitido(LaudoEmitidoEventoDto evento) {
        publicar(routingKeyLaudoEmitido, evento.eventoId(), evento);
    }

    private void publicar(String routingKey, UUID eventoId, Object evento) {
        try {
            rabbitTemplate.convertAndSend(exchange, routingKey, evento, mensagem -> {
                mensagem.getMessageProperties().setMessageId(eventoId.toString());
                mensagem.getMessageProperties().setContentType(MessageProperties.CONTENT_TYPE_JSON);
                return mensagem;
            });
            log.info("Evento {} publicado com id {}.", routingKey, eventoId);
        } catch (AmqpException ex) {
            log.error("Não foi possível publicar o evento {} ({}): broker indisponível.", routingKey, eventoId);
        }
    }
}
