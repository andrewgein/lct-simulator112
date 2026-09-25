package com.simulator112.adminservice.service;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Whitelist of compose services an admin may act on, and which per-service build-and-deploy
 * workflow file to dispatch for "update". Mirrors prod/docker-compose.yml service keys and the
 * .github/workflows/<name>.yml files - kept in sync by hand since both are small, fixed sets.
 */
public final class ControllableServices {

  public static final Map<String, String> UPDATE_WORKFLOW_BY_SERVICE = new LinkedHashMap<>();

  static {
    for (String service :
        new String[] {
          "auth-service", "incident-service", "course-service", "context-service",
          "review-service", "profile-service", "classifier-service", "notification-service",
          "admin-service", "dialog-service", "api-gateway", "frontend"
        }) {
      UPDATE_WORKFLOW_BY_SERVICE.put(service, service + ".yml");
    }
  }

  public static final String CONTROL_WORKFLOW = "service-control.yml";

  private ControllableServices() {}
}
