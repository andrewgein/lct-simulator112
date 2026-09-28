package com.simulator112.adminservice.domain.model;

import java.util.LinkedHashMap;
import java.util.Map;

public final class ContainerNames {

  public static final Map<String, String> CONTAINER_BY_SERVICE = new LinkedHashMap<>();

  static {
    CONTAINER_BY_SERVICE.put("auth-service", "sim112-stable-auth");
    CONTAINER_BY_SERVICE.put("incident-service", "sim112-stable-incident");
    CONTAINER_BY_SERVICE.put("course-service", "sim112-stable-course");
    CONTAINER_BY_SERVICE.put("context-service", "sim112-stable-context");
    CONTAINER_BY_SERVICE.put("review-service", "sim112-stable-review");
    CONTAINER_BY_SERVICE.put("profile-service", "sim112-stable-profile");
    CONTAINER_BY_SERVICE.put("classifier-service", "sim112-stable-classifier");
    CONTAINER_BY_SERVICE.put("notification-service", "sim112-stable-notification");
    CONTAINER_BY_SERVICE.put("admin-service", "sim112-stable-admin");
    CONTAINER_BY_SERVICE.put("dialog-service", "sim112-stable-dialog");
    CONTAINER_BY_SERVICE.put("api-gateway", "sim112-stable-gateway");
    CONTAINER_BY_SERVICE.put("frontend", "sim112-stable-frontend");
  }

  private ContainerNames() {}
}
