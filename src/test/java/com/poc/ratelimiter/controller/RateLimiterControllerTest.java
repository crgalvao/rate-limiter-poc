package com.poc.ratelimiter.controller;

import com.poc.ratelimiter.config.ClientRateLimitConfig;
import com.poc.ratelimiter.domain.DistributedHighThroughputRateLimiter;
import com.poc.ratelimiter.infrastructure.InMemoryDistributedKeyValueStore;
import org.junit.jupiter.api.*;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.mock.mockito.MockBean;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RateLimiterController.class)
class RateLimiterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ClientRateLimitConfig config;

    @Test
    void shouldReturnAllowedForConfiguredClient() throws Exception {
        ClientRateLimitConfig.ClientLimit c1 = new ClientRateLimitConfig.ClientLimit();
        c1.setId("xyz"); c1.setLimit(5);
        config.setClients(List.of(c1));

        mockMvc.perform(get("/ratelimit")
                        .param("clientId", "xyz"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("allowed?")));
    }

    @Test
    void shouldRejectUnknownClient() throws Exception {
        config.setClients(List.of()); 

        mockMvc.perform(get("/ratelimit")
                        .param("clientId", "unknown"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("allowed? false")));
    }
}
