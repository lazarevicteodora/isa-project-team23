package rs.ac.uns.ftn.isa.isa_project.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
public class HealthController {

    @Autowired
    private DataSource dataSource;

    @Value("${replica.id:unknown}")
    private String replicaId;

    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> health = new HashMap<>();

        try {
            // Proveri konekciju ka bazi
            try (Connection connection = dataSource.getConnection()) {
                boolean isValid = connection.isValid(5); // 5 sekundi timeout

                if (isValid) {
                    health.put("status", "healthy");
                    health.put("database", "connected");
                } else {
                    health.put("status", "unhealthy");
                    health.put("database", "connection_invalid");
                    return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(health);
                }
            }

            health.put("replica", replicaId);
            health.put("timestamp", LocalDateTime.now().toString());
            health.put("uptime", getUptime());

            return ResponseEntity.ok(health);

        } catch (Exception e) {
            health.put("status", "unhealthy");
            health.put("error", e.getMessage());
            health.put("replica", replicaId);
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(health);
        }
    }

    private String getUptime() {
        long uptimeMs = java.lang.management.ManagementFactory.getRuntimeMXBean().getUptime();
        long seconds = uptimeMs / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        return String.format("%dh %dm %ds", hours, minutes % 60, seconds % 60);
    }
}
