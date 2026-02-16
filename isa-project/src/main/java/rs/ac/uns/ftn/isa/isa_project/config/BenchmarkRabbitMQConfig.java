package rs.ac.uns.ftn.isa.isa_project.config;

import org.springframework.amqp.core.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BenchmarkRabbitMQConfig {

    @Value("${rabbitmq.benchmark.json.queue:upload.events.json.queue}")
    private String jsonQueue;

    @Value("${rabbitmq.benchmark.protobuf.queue:upload.events.protobuf.queue}")
    private String protobufQueue;

    @Value("${rabbitmq.benchmark.exchange:upload.events.exchange}")
    private String benchmarkExchange;

    @Bean
    public Queue uploadEventsJsonQueue() {
        return QueueBuilder.durable(jsonQueue).build();
    }

    @Bean
    public Queue uploadEventsProtobufQueue() {
        return QueueBuilder.durable(protobufQueue).build();
    }

    @Bean
    public DirectExchange uploadEventsExchange() {
        return new DirectExchange(benchmarkExchange);
    }

    @Bean
    public Binding jsonQueueBinding(Queue uploadEventsJsonQueue, DirectExchange uploadEventsExchange) {
        return BindingBuilder.bind(uploadEventsJsonQueue)
                .to(uploadEventsExchange)
                .with("upload.json");
    }

    @Bean
    public Binding protobufQueueBinding(Queue uploadEventsProtobufQueue, DirectExchange uploadEventsExchange) {
        return BindingBuilder.bind(uploadEventsProtobufQueue)
                .to(uploadEventsExchange)
                .with("upload.protobuf");
    }
}