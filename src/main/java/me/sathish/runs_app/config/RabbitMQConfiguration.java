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
import org.springframework.context.annotation.Profile;

@Configuration
@Slf4j
public class RabbitMQConfiguration {

    // Garmin events configuration - must match eventstracker's RabbitSchemaConfig
    public static final String GARMIN_API_QUEUE = "q.sathishprojects.garmin.api.events";
    public static final String GARMIN_OPS_QUEUE = "q.sathishprojects.garmin.ops.events";
    public static final String GARMIN_EXCHANGE = "x.sathishprojects.garmin.events.exchange";
    public static final String GARMIN_API_ROUTING_KEY = "sathishprojects.garmin.api.event";
    public static final String GARMIN_OPS_ROUTING_KEY = "sathishprojects.garmin.ops.event";

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
    @Profile("!test")
    public ApplicationRunner garminQueueValidator(AmqpAdmin amqpAdmin) {
        return new ApplicationRunner() {
            @Override
            public void run(ApplicationArguments args) {
                var apiProps = amqpAdmin.getQueueProperties(GARMIN_API_QUEUE);
                if (apiProps == null) {
                    throw new IllegalStateException(
                            "Garmin API queue '%s' not found. Ensure eventstracker provisions it before runs-app starts.".formatted(GARMIN_API_QUEUE));
                }
                log.info("Validated Garmin API queue exists: {}", GARMIN_API_QUEUE);
                
                var opsProps = amqpAdmin.getQueueProperties(GARMIN_OPS_QUEUE);
                if (opsProps == null) {
                    throw new IllegalStateException(
                            "Garmin OPS queue '%s' not found. Ensure eventstracker provisions it before runs-app starts.".formatted(GARMIN_OPS_QUEUE));
                }
                log.info("Validated Garmin OPS queue exists: {}", GARMIN_OPS_QUEUE);
            }
        };
    }
}
