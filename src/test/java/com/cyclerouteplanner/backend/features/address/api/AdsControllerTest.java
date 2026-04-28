package com.cyclerouteplanner.backend.features.address.api;

import com.cyclerouteplanner.backend.features.address.application.AdsService;
import com.cyclerouteplanner.backend.features.address.domain.AdsAddressSuggestion;
import com.cyclerouteplanner.backend.features.address.domain.AdsConnectivityStatus;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.List;

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
    void searchReturnsMappedSuggestionsWhenAdsIsReachable() throws Exception {
        AdsService adsService = mock(AdsService.class);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AdsController(adsService)).build();
        when(adsService.search("tartu", 3)).thenReturn(List.of(new AdsAddressSuggestion(
                "ME01087725",
                "Mustamäe tee 51, Kristiine linnaosa, Tallinn, Harju maakond",
                "Mustamäe tee 51",
                "Kristiine linnaosa",
                "Tallinn",
                "Harju maakond",
                59.421047,
                24.697966
        )));

        mockMvc.perform(get("/api/address/search")
                        .queryParam("query", "tartu")
                        .queryParam("limit", "3")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").value("ME01087725"))
                .andExpect(jsonPath("$[0].label").value("Mustamäe tee 51, Kristiine linnaosa, Tallinn, Harju maakond"))
                .andExpect(jsonPath("$[0].address").value("Mustamäe tee 51"))
                .andExpect(jsonPath("$[0].municipality").value("Tallinn"))
                .andExpect(jsonPath("$[0].lat").value(59.421047))
                .andExpect(jsonPath("$[0].lon").value(24.697966))
                .andExpect(jsonPath("$[0].payloadSnippet").doesNotExist());
    }

    @Test
    void searchReturnsBadRequestWhenValidationFails() throws Exception {
        AdsService adsService = mock(AdsService.class);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AdsController(adsService)).build();
        when(adsService.search("t", 3)).thenThrow(new IllegalArgumentException("query must be at least 2 characters"));

        mockMvc.perform(get("/api/address/search")
                        .queryParam("query", "t")
                        .queryParam("limit", "3")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    void searchReturnsServiceUnavailableWhenAdsIsDown() throws Exception {
        AdsService adsService = mock(AdsService.class);
        MockMvc mockMvc = MockMvcBuilders.standaloneSetup(new AdsController(adsService)).build();
        when(adsService.search("tartu", 3)).thenThrow(new IllegalStateException("ADS unavailable"));

        mockMvc.perform(get("/api/address/search")
                        .queryParam("query", "tartu")
                        .queryParam("limit", "3")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isServiceUnavailable());
    }
}
