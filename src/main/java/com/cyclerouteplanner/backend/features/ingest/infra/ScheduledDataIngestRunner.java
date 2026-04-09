package com.cyclerouteplanner.backend.features.ingest.infra;

import com.cyclerouteplanner.backend.features.ingest.application.DataIngestService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@Profile({"local", "docker"})
@ConditionalOnProperty(prefix = "ingest.scheduler", name = "enabled", havingValue = "true")
public class ScheduledDataIngestRunner {

    private static final Logger LOG = LoggerFactory.getLogger(ScheduledDataIngestRunner.class);
    private final DataIngestService dataIngestService;

    public ScheduledDataIngestRunner(DataIngestService dataIngestService) {
        this.dataIngestService = dataIngestService;
    }

    @Scheduled(cron = "${ingest.scheduler.cron:0 0 */6 * * *}")
    public void run() {
        int snapshots = dataIngestService.runSkeletonIngest().size();
        LOG.info("Skeleton ingest run finished, snapshots updated={}", snapshots);
    }
}
