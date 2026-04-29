package com.cyclerouteplanner.backend.features.address.api;

import com.cyclerouteplanner.backend.features.address.api.dto.response.AdsConnectivityResponse;
import com.cyclerouteplanner.backend.features.address.api.dto.response.AdsAddressSuggestionResponse;
import com.cyclerouteplanner.backend.features.address.application.AdsService;
import com.cyclerouteplanner.backend.features.address.domain.AdsAddressSuggestion;
import com.cyclerouteplanner.backend.features.address.domain.AdsConnectivityStatus;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/address")
@CrossOrigin(originPatterns = {"http://localhost:*", "http://127.0.0.1:*", "https://baisikkel.models.ee"}, allowCredentials = "true")
public class AdsController {

    private final AdsService adsService;

    public AdsController(AdsService adsService) {
        this.adsService = adsService;
    }

    /**
     * Lightweight health-style probe for Maa-amet ADS availability used during integration setup and diagnostics.
     */
    @GetMapping("/connectivity")
    public ResponseEntity<AdsConnectivityResponse> connectivity() {
        AdsConnectivityStatus status = adsService.checkConnectivity();
        AdsConnectivityResponse response = new AdsConnectivityResponse(
                status.provider(),
                status.reachable(),
                status.details(),
                status.checkedAt()
        );

        if (status.reachable()) {
            return ResponseEntity.ok(response);
        }
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(response);
    }

    @GetMapping("/search")
    public List<AdsAddressSuggestionResponse> search(
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int limit
    ) {
        try {
            return adsService.search(query, limit).stream()
                    .map(this::toResponse)
                    .toList();
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, ex.getMessage(), ex);
        } catch (RuntimeException ex) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, ex.getMessage(), ex);
        }
    }

    private AdsAddressSuggestionResponse toResponse(AdsAddressSuggestion suggestion) {
        return new AdsAddressSuggestionResponse(
                suggestion.id(),
                suggestion.label(),
                suggestion.address(),
                suggestion.settlement(),
                suggestion.municipality(),
                suggestion.county(),
                suggestion.latitude(),
                suggestion.longitude()
        );
    }
}
