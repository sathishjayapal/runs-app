package me.sathish.runs_app.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpAdmin;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class RabbitMQConfiguration {

    // Garmin events configuration - must match eventstracker's RabbitSchemaConfig
    public static final String GARMIN_QUEUE = "q.sathishprojects.garmin.api.events";
    public static final String GARMIN_EXCHANGE = "x.sathishprojects.garmin.events.exchange";
    public static final String GARMIN_ROUTING_KEY = "sathishprojects.garmin.api.event";

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, MessageConverter messageConverter) {
        log.info("Configuring RabbitTemplate with ConnectionFactory");
        final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(messageConverter);
        
        // Enable publisher confirms for debugging
        rabbitTemplate.setMandatory(true);
        
        // Add return callback to detect unroutable messages
        rabbitTemplate.setReturnsCallback(returned -> {
            log.error("Message returned - NOT ROUTED! Exchange: {}, RoutingKey: {}, ReplyCode: {}, ReplyText: {}",
                returned.getExchange(), returned.getRoutingKey(), 
                returned.getReplyCode(), returned.getReplyText());
            log.error("Returned message: {}", new String(returned.getMessage().getBody()));
        });
        
        log.info("RabbitTemplate configured successfully");
        return rabbitTemplate;
    }

    @Bean
    public MessageConverter messageConverter() {
        return new JacksonJsonMessageConverter();
    }

    @Bean
    public ApplicationRunner garminQueueValidator(AmqpAdmin amqpAdmin) {
        return new ApplicationRunner() {
            @Override
            public void run(ApplicationArguments args) {
                var props = amqpAdmin.getQueueProperties(GARMIN_QUEUE);
                if (props == null) {
                    throw new IllegalStateException(
                            "Garmin queue '%s' not found. Ensure eventstracker provisions it before runs-app starts.".formatted(GARMIN_QUEUE));
                }
                log.info("Validated Garmin queue exists: {}", GARMIN_QUEUE);
            }
        };
    }
}
