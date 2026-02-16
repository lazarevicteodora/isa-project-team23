package rs.ac.uns.ftn.isa.isa_project.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import rs.ac.uns.ftn.isa.isa_project.service.ETLPipelineService;

@Component
public class ETLScheduler {

    private static final Logger LOG = LoggerFactory.getLogger(ETLScheduler.class);

    @Autowired
    private ETLPipelineService etlPipelineService;

    /**
     * Pokreće ETL pipeline jednom dnevno u ponoć.
     * cron = "sekunde minute sati dan mesec dan-u-nedelji"
     */
    @Scheduled(cron = "0 0 0 * * *")
    public void runDailyETL() {
        LOG.info("[ETL Scheduler] Triggering daily ETL pipeline...");
        try {
            etlPipelineService.runPipeline();
        } catch (Exception e) {
            LOG.error("[ETL Scheduler] Pipeline failed: {}", e.getMessage());
        }
    }
}