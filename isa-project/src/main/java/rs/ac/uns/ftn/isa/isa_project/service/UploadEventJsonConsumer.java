package rs.ac.uns.ftn.isa.isa_project.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import rs.ac.uns.ftn.isa.isa_project.dto.UploadEvent;

import java.util.ArrayList;
import java.util.List;

@Component
public class UploadEventJsonConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(UploadEventJsonConsumer.class);

    @Autowired
    private ObjectMapper objectMapper;

    private final List<Long> deserializationTimes = new ArrayList<>();

    @RabbitListener(queues = "${rabbitmq.benchmark.json.queue:upload.events.json.queue}")
    public void handleJsonMessage(UploadEvent event) {
        long startTime = System.nanoTime();

        // Poruka je već deserijalizovana od strane RabbitMQ (Jackson)
        // Ali merimo dodatnu deserijalizaciju za realističniji test
        try {
            byte[] jsonBytes = objectMapper.writeValueAsBytes(event);
            UploadEvent deserialized = objectMapper.readValue(jsonBytes, UploadEvent.class);

            long endTime = System.nanoTime();
            long duration = endTime - startTime;

            deserializationTimes.add(duration);

            LOG.debug("JSON deserialization: {} ns (video: {})", duration, deserialized.getVideoId());

        } catch (Exception e) {
            LOG.error("JSON deserialization error: {}", e.getMessage());
        }
    }

    public double getAverageDeserializationTimeMs() {
        if (deserializationTimes.isEmpty()) return 0.0;
        return deserializationTimes.stream()
                .mapToLong(Long::longValue)
                .average()
                .orElse(0.0) / 1_000_000.0;
    }

    public void reset() {
        deserializationTimes.clear();
    }
}