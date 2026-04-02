package com.cyclerouteplanner.backend.features.address.api;

import com.cyclerouteplanner.backend.features.address.application.AdsService;
import com.cyclerouteplanner.backend.features.address.domain.AdsConnectivityStatus;
import com.cyclerouteplanner.backend.features.address.domain.AdsSearchStatus;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class AdsControllerTest {

    @Test
    void connectivityReturnsOkWhenAdsIsReachable() throws Exception {
        AdsService adsService = mock(AdsService.class);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AdsController(adsService)).build();
        when(adsService.checkConnectivity())
                .thenReturn(new AdsConnectivityStatus(true, "maa-amet-ads", "ADS OK", Instant.parse("2026-01-01T00:00:00Z")));

        mockMvc.perform(get("/api/address/connectivity").accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.provider").value("maa-amet-ads"))
                .andExpect(jsonPath("$.reachable").value(true))
                .andExpect(jsonPath("$.details").value("ADS OK"));
    }

    @Test
    void searchReturnsServiceUnavailableWhenAdsIsDown() throws Exception {
        AdsService adsService = mock(AdsService.class);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AdsController(adsService)).build();
        when(adsService.search("tartu", 3))
                .thenReturn(new AdsSearchStatus(
                        false,
                        "maa-amet-ads",
                        "tartu",
                        3,
                        "ADS unavailable",
                        null,
                        Instant.parse("2026-01-01T00:00:00Z")
                ));

        mockMvc.perform(get("/api/address/search")
                        .queryParam("query", "tartu")
                        .queryParam("limit", "3")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isServiceUnavailable())
                .andExpect(jsonPath("$.provider").value("maa-amet-ads"))
                .andExpect(jsonPath("$.reachable").value(false))
                .andExpect(jsonPath("$.query").value("tartu"))
                .andExpect(jsonPath("$.limit").value(3))
                .andExpect(jsonPath("$.details").value("ADS unavailable"));
    }
}
