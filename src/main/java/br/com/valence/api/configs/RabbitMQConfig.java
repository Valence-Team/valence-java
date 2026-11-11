package br.com.valence.api.configs;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Value("${valence.rabbitmq.exchange}")
    private String exchange;

    @Value("${valence.rabbitmq.fila-alertas}")
    private String filaAlertas;

    @Value("${valence.rabbitmq.rk-ocorrencia-triada}")
    private String routingKeyOcorrenciaTriada;

    @Bean
    public TopicExchange eventosExchange() {
        return new TopicExchange(exchange, true, false);
    }

    @Bean
    public Queue filaAlertas() {
        return new Queue(filaAlertas, true);
    }

    @Bean
    public Binding ligacaoFilaAlertas(Queue filaAlertas, TopicExchange eventosExchange) {
        return BindingBuilder.bind(filaAlertas).to(eventosExchange).with(routingKeyOcorrenciaTriada);
    }

    @Bean
    public Jackson2JsonMessageConverter messageConverter(ObjectMapper objectMapper) {
        Jackson2JsonMessageConverter conversor = new Jackson2JsonMessageConverter(objectMapper);
        conversor.setAlwaysConvertToInferredType(true);
        return conversor;
    }
}
