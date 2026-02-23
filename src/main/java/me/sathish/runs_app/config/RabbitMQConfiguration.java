package me.sathish.runs_app.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.TopicExchange;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfiguration {

    public static final String GARMIN_QUEUE = "x.garmin.operations";
    public static final String GARMIN_EXCHANGE = "x.sathishprojects.events";
    public static final String GARMIN_ROUTING_KEY = "garmin.operations.crud";

    @Bean
    public Queue garminQueue() {
        return new Queue(GARMIN_QUEUE, true);
    }

    @Bean
    public TopicExchange garminExchange() {
        return new TopicExchange(GARMIN_EXCHANGE);
    }

    @Bean
    public Binding garminBinding(Queue garminQueue, TopicExchange garminExchange) {
        return BindingBuilder.bind(garminQueue).to(garminExchange).with(GARMIN_ROUTING_KEY);
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        return rabbitTemplate;
    }

    @Bean
    public MessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }
}
