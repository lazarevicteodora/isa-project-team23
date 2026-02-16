package rs.ac.uns.ftn.isa.isa_project.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.JacksonJsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;


@Configuration
public class TranscodingRabbitMQConfig {

    // Main transcoding queue
    @Value("${rabbitmq.transcoding.queue:video.transcoding.queue}")
    private String transcodingQueue;

    // Dead Letter Queue
    @Value("${rabbitmq.transcoding.dlq:video.transcoding.dlq}")
    private String transcodingDLQ;

    // Exchange
    @Value("${rabbitmq.transcoding.exchange:video.transcoding.exchange}")
    private String transcodingExchange;

    // Routing keys
    @Value("${rabbitmq.transcoding.routingkey:video.transcoding}")
    private String transcodingRoutingKey;

    @Value("${rabbitmq.transcoding.dlq.routingkey:video.transcoding.dlq}")
    private String dlqRoutingKey;

    // RabbitMQ connection settings
    @Value("${spring.rabbitmq.host:localhost}")
    private String host;

    @Value("${spring.rabbitmq.port:5672}")
    private int port;

    @Value("${spring.rabbitmq.username:guest}")
    private String username;

    @Value("${spring.rabbitmq.password:guest}")
    private String password;

    /**
     * Dead Letter Queue - prima poruke koje nisu uspešno obrađene
     */
    @Bean
    public Queue transcodingDLQ() {
        return QueueBuilder.durable(transcodingDLQ).build();
    }

    @Bean
    public Queue transcodingQueue() {
        return QueueBuilder.durable(transcodingQueue)
                .withArgument("x-dead-letter-exchange", transcodingExchange)
                .withArgument("x-dead-letter-routing-key", dlqRoutingKey)
                .withArgument("x-message-ttl", 3600000) // 1 sat
                .build();
    }

    /**
     * Direct Exchange za rutiranje poruka
     */
    @Bean
    public DirectExchange transcodingExchange() {
        return new DirectExchange(transcodingExchange);
    }

    /**
     * Binding između main queue-a i exchange-a
     */
    @Bean
    public Binding transcodingBinding(Queue transcodingQueue, DirectExchange transcodingExchange) {
        return BindingBuilder.bind(transcodingQueue)
                .to(transcodingExchange)
                .with(transcodingRoutingKey);
    }

    /**
     * Binding između DLQ i exchange-a
     */
    @Bean
    public Binding dlqBinding(Queue transcodingDLQ, DirectExchange transcodingExchange) {
        return BindingBuilder.bind(transcodingDLQ)
                .to(transcodingExchange)
                .with(dlqRoutingKey);
    }

    /**
     * ConnectionFactory za povezivanje sa RabbitMQ serverom
     */
    @Bean
    public ConnectionFactory connectionFactory() {
        CachingConnectionFactory factory = new CachingConnectionFactory(host, port);
        factory.setUsername(username);
        factory.setPassword(password);
        return factory;
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new JacksonJsonMessageConverter();
    }


    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jsonMessageConverter());
        return template;
    }

    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter());
        factory.setPrefetchCount(1);

        return factory;
    }
}