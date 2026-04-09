package com.cyclerouteplanner.backend.features.ingest.api;

import com.cyclerouteplanner.backend.features.ingest.api.dto.response.DataSnapshotResponse;
import com.cyclerouteplanner.backend.features.ingest.application.DataIngestService;
import com.cyclerouteplanner.backend.features.ingest.domain.DataSnapshotRecord;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/ingest")
@Profile({"local", "docker"})
@ConditionalOnProperty(prefix = "api", name = "dev-endpoints-enabled", havingValue = "true")
public class DataIngestController {

    private final DataIngestService dataIngestService;

    public DataIngestController(DataIngestService dataIngestService) {
        this.dataIngestService = dataIngestService;
    }

    @PostMapping("/run")
    public List<DataSnapshotResponse> runSkeletonIngest() {
        return dataIngestService.runSkeletonIngest().stream()
                .map(this::toResponse)
                .toList();
    }

    @GetMapping("/snapshots")
    public List<DataSnapshotResponse> latestSnapshots(@RequestParam(defaultValue = "20") int limit) {
        return dataIngestService.latestSnapshots(limit).stream()
                .map(this::toResponse)
                .toList();
    }

    private DataSnapshotResponse toResponse(DataSnapshotRecord snapshot) {
        return new DataSnapshotResponse(
                snapshot.source(),
                snapshot.sourceVersion(),
                snapshot.sourceTimestamp(),
                snapshot.checksum(),
                snapshot.metadata(),
                snapshot.createdAt()
        );
    }
}
