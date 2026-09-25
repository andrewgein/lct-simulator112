package com.simulator112.adminservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.LinkedHashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Pulls a handful of known, non-secret config keys from other services' actuator /env endpoints
 * (gateway, auth-service) and dialog-service's small /internal/settings endpoint, for the
 * read-only "current configuration" admin page. Every call is direct, container-to-container -
 * never routed through the gateway's own proxy routes.
 */
@Slf4j
@Component
public class SettingsService {

  private static final String[] GATEWAY_KEYS = {
    "cors.allowed-origins", "rate-limit.replenish-rate", "rate-limit.burst-capacity"
  };
  private static final String[] AUTH_KEYS = {
    "jwt.access-token-expiration", "jwt.refresh-token-expiration"
  };

  private final RestClient gatewayClient;
  private final RestClient authClient;
  private final RestClient dialogClient;

  public SettingsService(
      @Value("${settings.gateway-base-url}") String gatewayBaseUrl,
      @Value("${settings.auth-base-url}") String authBaseUrl,
      @Value("${settings.dialog-base-url}") String dialogBaseUrl) {
    this.gatewayClient = RestClient.builder().baseUrl(gatewayBaseUrl).build();
    this.authClient = RestClient.builder().baseUrl(authBaseUrl).build();
    this.dialogClient = RestClient.builder().baseUrl(dialogBaseUrl).build();
  }

  public Map<String, Object> gatewaySettings() {
    return extractEnvKeys(gatewayClient, GATEWAY_KEYS);
  }

  public Map<String, Object> authSettings() {
    return extractEnvKeys(authClient, AUTH_KEYS);
  }

  public Map<String, Object> dialogSettings() {
    try {
      JsonNode response = dialogClient.get().uri("/internal/settings").retrieve().body(JsonNode.class);
      Map<String, Object> result = new LinkedHashMap<>();
      if (response != null) response.fields().forEachRemaining(e -> result.put(e.getKey(), e.getValue().asText(null)));
      return result;
    } catch (Exception e) {
      log.warn("Failed to fetch dialog-service settings: {}", e.getMessage());
      return Map.of();
    }
  }

  private Map<String, Object> extractEnvKeys(RestClient client, String[] keys) {
    Map<String, Object> result = new LinkedHashMap<>();
    try {
      JsonNode response = client.get().uri("/actuator/env").retrieve().body(JsonNode.class);
      if (response == null) return result;
      for (String key : keys) {
        JsonNode value = findPropertyValue(response, key);
        result.put(key, value != null ? value.asText(null) : null);
      }
    } catch (Exception e) {
      log.warn("Failed to fetch /actuator/env: {}", e.getMessage());
    }
    return result;
  }

  /** /actuator/env's "propertySources" is ordered by precedence - the first source that defines
   * the key wins, matching how Spring itself resolves the effective value. */
  private JsonNode findPropertyValue(JsonNode envResponse, String key) {
    for (JsonNode source : envResponse.path("propertySources")) {
      JsonNode property = source.path("properties").path(key).path("value");
      if (!property.isMissingNode()) return property;
    }
    return null;
  }
}
