
package com.poc.ratelimiter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@ConfigurationProperties(prefix = "ratelimiter")
public class ClientRateLimitConfig {

    private List<ClientLimit> clients = Collections.emptyList();

    public Map<String, Integer> getClientLimits() {
        if (clients == null || clients.isEmpty()) {
            return Collections.emptyMap();
        }
        return clients.stream().collect(Collectors.toMap(ClientLimit::getId, ClientLimit::getLimit));
    }

    public List<ClientLimit> getClients() { return clients; }
    public void setClients(List<ClientLimit> clients) { this.clients = clients; }

    public static class ClientLimit {
        private String id;
        private int limit;

        public String getId() { return id; }
        public void setId(String id) { this.id = id; }
        public int getLimit() { return limit; }
        public void setLimit(int limit) { this.limit = limit; }
    }
}
