package rs.ac.uns.ftn.isa.isa_project.service;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import rs.ac.uns.ftn.isa.isa_project.repository.ViewLogRepository;

import java.time.LocalDate;
import java.util.concurrent.atomic.AtomicLong;

@Service
public class ActiveUserMetricsService {

    @Autowired
    private MeterRegistry meterRegistry;

    @Autowired
    private ViewLogRepository viewLogRepository;

    private final AtomicLong activeUserCount = new AtomicLong(0);

    @PostConstruct
    public void init() {
        // Registruj custom Gauge metriku
        Gauge.builder("app.active.users.24h", activeUserCount, AtomicLong::get)
                .description("Broj aktivnih korisnika u poslednjih 24h")
                .register(meterRegistry);
    }

    /**
     * Poziva se pri svakom zahtevu da azurira broj aktivnih korisnika.
     * Aktivni korisnik = onaj koji je pogledao bar jedan video danas.
     */
    public void updateActiveUsers() {
        long count = viewLogRepository.countDistinctUsersToday(LocalDate.now());
        activeUserCount.set(count);
    }
}