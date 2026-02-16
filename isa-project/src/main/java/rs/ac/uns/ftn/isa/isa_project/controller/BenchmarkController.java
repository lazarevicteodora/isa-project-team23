package rs.ac.uns.ftn.isa.isa_project.controller;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import rs.ac.uns.ftn.isa.isa_project.service.BenchmarkService;

@RestController
@RequestMapping("/api/benchmark")
public class BenchmarkController {

    private static final Logger LOG = LoggerFactory.getLogger(BenchmarkController.class);

    @Autowired
    private BenchmarkService benchmarkService;

    @PostMapping("/run")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<BenchmarkResponse> runBenchmark(
            @RequestParam(defaultValue = "50") int messageCount) throws InterruptedException {

        LOG.info("Starting benchmark with {} messages", messageCount);

        BenchmarkService.BenchmarkResult result = benchmarkService.runBenchmark(messageCount);

        BenchmarkResponse response = new BenchmarkResponse(
                result.jsonResults.format,
                result.jsonResults.avgSerializationTimeMs,
                result.jsonResults.avgDeserializationTimeMs,
                result.jsonResults.avgMessageSizeBytes,
                result.protobufResults.format,
                result.protobufResults.avgSerializationTimeMs,
                result.protobufResults.avgDeserializationTimeMs,
                result.protobufResults.avgMessageSizeBytes,
                messageCount
        );

        LOG.info("Benchmark completed. JSON: ser={:.3f}ms deser={:.3f}ms size={}B | Protobuf: ser={:.3f}ms deser={:.3f}ms size={}B",
                response.jsonAvgSerializationMs,
                response.jsonAvgDeserializationMs,
                response.jsonAvgSizeBytes,
                response.protobufAvgSerializationMs,
                response.protobufAvgDeserializationMs,
                response.protobufAvgSizeBytes);

        return ResponseEntity.ok(response);
    }

    public static class BenchmarkResponse {
        public String jsonFormat;
        public double jsonAvgSerializationMs;
        public double jsonAvgDeserializationMs;
        public double jsonAvgSizeBytes;

        public String protobufFormat;
        public double protobufAvgSerializationMs;
        public double protobufAvgDeserializationMs;
        public double protobufAvgSizeBytes;

        public int messageCount;

        public double sizeDifferencePercent;
        public double serializationSpeedupPercent;
        public double deserializationSpeedupPercent;

        public BenchmarkResponse(String jsonFormat,
                                 double jsonAvgSerMs, double jsonAvgDeserMs, double jsonAvgSize,
                                 String protobufFormat,
                                 double protobufAvgSerMs, double protobufAvgDeserMs, double protobufAvgSize,
                                 int messageCount) {
            this.jsonFormat = jsonFormat;
            this.jsonAvgSerializationMs = jsonAvgSerMs;
            this.jsonAvgDeserializationMs = jsonAvgDeserMs;
            this.jsonAvgSizeBytes = jsonAvgSize;

            this.protobufFormat = protobufFormat;
            this.protobufAvgSerializationMs = protobufAvgSerMs;
            this.protobufAvgDeserializationMs = protobufAvgDeserMs;
            this.protobufAvgSizeBytes = protobufAvgSize;

            this.messageCount = messageCount;

            // Izračunaj razlike
            this.sizeDifferencePercent = ((jsonAvgSize - protobufAvgSize) / jsonAvgSize) * 100;
            this.serializationSpeedupPercent = ((jsonAvgSerMs - protobufAvgSerMs) / jsonAvgSerMs) * 100;
            this.deserializationSpeedupPercent = ((jsonAvgDeserMs - protobufAvgDeserMs) / jsonAvgDeserMs) * 100;
        }
    }
}