package rs.ac.uns.ftn.isa.isa_project.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.protobuf.util.Timestamps;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import rs.ac.uns.ftn.isa.isa_project.dto.UploadEvent;
import rs.ac.uns.ftn.isa.isa_project.proto.UploadEventProto;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
public class BenchmarkService {

    private static final Logger LOG = LoggerFactory.getLogger(BenchmarkService.class);

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private ObjectMapper objectMapper;

    @Value("${rabbitmq.benchmark.exchange:upload.events.exchange}")
    private String exchange;

    public BenchmarkResult runBenchmark(int messageCount) {
        LOG.info("Starting benchmark with {} messages", messageCount);

        BenchmarkResult result = new BenchmarkResult();

        // JSON Benchmark
        result.jsonResults = benchmarkJson(messageCount);

        // Protobuf Benchmark
        result.protobufResults = benchmarkProtobuf(messageCount);

        LOG.info("Benchmark completed!");
        return result;
    }

    private FormatResult benchmarkJson(int messageCount) {
        LOG.info("Benchmarking JSON format...");

        List<Long> serializationTimes = new ArrayList<>();
        List<Integer> messageSizes = new ArrayList<>();

        for (int i = 0; i < messageCount; i++) {
            UploadEvent event = createSampleEvent(i);

            long startTime = System.nanoTime();
            try {
                byte[] jsonBytes = objectMapper.writeValueAsBytes(event);
                long endTime = System.nanoTime();

                serializationTimes.add(endTime - startTime);
                messageSizes.add(jsonBytes.length);

                // Pošalji u queue
                rabbitTemplate.convertAndSend(exchange, "upload.json", event);

            } catch (Exception e) {
                LOG.error("JSON serialization error: {}", e.getMessage());
            }
        }

        return new FormatResult("JSON", serializationTimes, messageSizes);
    }

    private FormatResult benchmarkProtobuf(int messageCount) {
        LOG.info("Benchmarking Protobuf format...");

        List<Long> serializationTimes = new ArrayList<>();
        List<Integer> messageSizes = new ArrayList<>();

        for (int i = 0; i < messageCount; i++) {
            UploadEvent event = createSampleEvent(i);

            long startTime = System.nanoTime();
            try {
                UploadEventProto.UploadEvent protoEvent = convertToProto(event);
                byte[] protoBytes = protoEvent.toByteArray();
                long endTime = System.nanoTime();

                serializationTimes.add(endTime - startTime);
                messageSizes.add(protoBytes.length);

                // Pošalji u queue kao byte array
                rabbitTemplate.convertAndSend(exchange, "upload.protobuf", protoBytes);

            } catch (Exception e) {
                LOG.error("Protobuf serialization error: {}", e.getMessage());
            }
        }

        return new FormatResult("Protobuf", serializationTimes, messageSizes);
    }

    private UploadEvent createSampleEvent(int index) {
        return new UploadEvent(
                (long) index,
                "Sample Video " + index,
                1024L * 1024 * 50, // 50MB
                "user" + index,
                "/storage/videos/video_" + index + ".mp4",
                LocalDateTime.now()
        );
    }

    private UploadEventProto.UploadEvent convertToProto(UploadEvent event) {
        return UploadEventProto.UploadEvent.newBuilder()
                .setVideoId(event.getVideoId())
                .setTitle(event.getTitle())
                .setFileSize(event.getFileSize())
                .setAuthorUsername(event.getAuthorUsername())
                .setVideoPath(event.getVideoPath())
                .setUploadedAt(event.getUploadedAt().toString())
                .build();
    }

    public static class BenchmarkResult {
        public FormatResult jsonResults;
        public FormatResult protobufResults;
    }

    public static class FormatResult {
        public String format;
        public double avgSerializationTimeMs;
        public double avgMessageSizeBytes;
        public int messageCount;

        public FormatResult(String format, List<Long> serializationTimes, List<Integer> sizes) {
            this.format = format;
            this.messageCount = sizes.size();
            this.avgSerializationTimeMs = serializationTimes.stream()
                    .mapToLong(Long::longValue)
                    .average()
                    .orElse(0.0) / 1_000_000.0; // nano to milli
            this.avgMessageSizeBytes = sizes.stream()
                    .mapToInt(Integer::intValue)
                    .average()
                    .orElse(0.0);
        }
    }
}