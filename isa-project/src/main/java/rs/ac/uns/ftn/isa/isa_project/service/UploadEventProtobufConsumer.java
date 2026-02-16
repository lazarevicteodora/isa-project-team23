package rs.ac.uns.ftn.isa.isa_project.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;
import rs.ac.uns.ftn.isa.isa_project.proto.UploadEventProto;

import java.util.ArrayList;
import java.util.List;

@Component
public class UploadEventProtobufConsumer {

    private static final Logger LOG = LoggerFactory.getLogger(UploadEventProtobufConsumer.class);

    private final List<Long> deserializationTimes = new ArrayList<>();

    @RabbitListener(queues = "${rabbitmq.benchmark.protobuf.queue:upload.events.protobuf.queue}")
    public void handleProtobufMessage(byte[] data) {
        long startTime = System.nanoTime();

        try {
            UploadEventProto.UploadEvent event = UploadEventProto.UploadEvent.parseFrom(data);

            long endTime = System.nanoTime();
            long duration = endTime - startTime;

            deserializationTimes.add(duration);

            LOG.debug("Protobuf deserialization: {} ns (video: {})", duration, event.getVideoId());

        } catch (Exception e) {
            LOG.error("Protobuf deserialization error: {}", e.getMessage());
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