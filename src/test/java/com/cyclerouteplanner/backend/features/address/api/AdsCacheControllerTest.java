package com.cyclerouteplanner.backend.features.address.api;

import com.cyclerouteplanner.backend.features.address.application.AdsCacheRefreshService;
import com.cyclerouteplanner.backend.features.address.domain.AdsCacheRefreshStatus;
import com.cyclerouteplanner.backend.features.address.infra.AdsClientProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdsCacheControllerTest {

    @Test
    void refreshUsesConfiguredDefaultsWhenParamsMissing() throws Exception {
        AdsCacheRefreshService service = mock(AdsCacheRefreshService.class);
        AdsClientProperties properties = new AdsClientProperties();
        properties.setCacheRefreshDefaultQuery("Tallinn");
        properties.setCacheRefreshDefaultLimit(50);

        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AdsCacheController(service, properties)).build();
        when(service.refresh("Tallinn", 50)).thenReturn(new AdsCacheRefreshStatus(
                true,
                "maa-amet-ads",
                "Tallinn",
                50,
                12,
                "Address cache refresh completed",
                Instant.parse("2026-04-02T00:00:00Z")
        ));

        mockMvc.perform(post("/api/address/cache/refresh").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("maa-amet-ads"))
                .andExpect(jsonPath("$.query").value("Tallinn"))
                .andExpect(jsonPath("$.limit").value(50))
                .andExpect(jsonPath("$.upsertedCount").value(12));
    }

    @Test
    void refreshReturnsServiceUnavailableWhenAdsFails() throws Exception {
        AdsCacheRefreshService service = mock(AdsCacheRefreshService.class);
        AdsClientProperties properties = new AdsClientProperties();
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AdsCacheController(service, properties)).build();
        when(service.refresh("tartu", 5)).thenReturn(new AdsCacheRefreshStatus(
                false,
                "maa-amet-ads",
                "tartu",
                5,
                0,
                "ADS unavailable",
                Instant.parse("2026-04-02T00:00:00Z")
        ));

        mockMvc.perform(post("/api/address/cache/refresh")
                        .param("query", "tartu")
                        .param("limit", "5")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.provider").value("maa-amet-ads"))
                .andExpect(jsonPath("$.reachable").value(false))
                .andExpect(jsonPath("$.details").value("ADS unavailable"));
    }
}
