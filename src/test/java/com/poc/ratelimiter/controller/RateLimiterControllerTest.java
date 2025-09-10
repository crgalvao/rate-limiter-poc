package com.poc.ratelimiter.controller;

import com.poc.ratelimiter.config.ClientRateLimitConfig;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(BusinessController.class)
class RateLimiterControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ClientRateLimitConfig config;

    @Test
    void shouldReturnAllowedForConfiguredClient() throws Exception {
        ClientRateLimitConfig.ClientLimit c1 = new ClientRateLimitConfig.ClientLimit();
        c1.setId("xyz");
        c1.setLimit(5);
        
        

        mockMvc.perform(get("/api/products")
                        .header("X-Client-ID", "xyz"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Returning list of products for client: xyz")));
    }

    @Test
    void shouldRejectUnknownClient() throws Exception {
        
        

        mockMvc.perform(get("/api/products")
                        .header("X-Client-ID", "unknown"))
                .andExpect(status().isBadRequest());
    }
}