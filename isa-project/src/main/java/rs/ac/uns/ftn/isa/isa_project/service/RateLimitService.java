package rs.ac.uns.ftn.isa.isa_project.service;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class RateLimitService {

    private final Map<String, LoginAttempts> attemptMap = new ConcurrentHashMap<>();

    public synchronized boolean allowRequest(String ipAddress) {
        LoginAttempts attempts = attemptMap.computeIfAbsent(ipAddress, k -> new LoginAttempts());

        attempts.removeOldAttempts();

        if (attempts.getCount() >= 5) {
            return false;
        }

        attempts.addAttempt();
        return true;
    }

    public synchronized int getRemainingAttempts(String ipAddress) {
        LoginAttempts attempts = attemptMap.get(ipAddress);
        if (attempts == null) {
            return 5; // Nema pokušaja, svih 5 je dostupno
        }

        attempts.removeOldAttempts();
        return Math.max(0, 5 - attempts.getCount());
    }

    public synchronized void resetAttempts(String ipAddress) {
        attemptMap.remove(ipAddress);
    }

    private static class LoginAttempts {
        private final Map<LocalDateTime, Boolean> attempts = new ConcurrentHashMap<>();

        public void addAttempt() {
            attempts.put(LocalDateTime.now(), true);
        }

        public void removeOldAttempts() {
            LocalDateTime oneMinuteAgo = LocalDateTime.now().minusMinutes(1);
            attempts.keySet().removeIf(timestamp -> timestamp.isBefore(oneMinuteAgo));
        }

        public int getCount() {
            return attempts.size();
        }
    }
}