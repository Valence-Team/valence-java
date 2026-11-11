package br.com.valence.api.consumers;

import br.com.valence.api.dtos.eventos.OcorrenciaTriadaEventoDto;
import br.com.valence.api.services.AlertaService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class OcorrenciaTriadaConsumer {

    private final AlertaService alertaService;

    @RabbitListener(queues = "${valence.rabbitmq.fila-alertas}")
    public void receber(@Payload OcorrenciaTriadaEventoDto evento) {
        int criados = alertaService.gerarAlertasDeSuspeita(evento);
        log.info("Evento {} consumido da fila de alertas: {} alerta(s) criado(s).",
                evento == null ? null : evento.eventoId(), criados);
    }
}
