package com.simulator112.adminservice.domain.model;

import java.util.Map;

public record PrometheusSample(Map<String, String> labels, double value) {
  public String label(String name) {
    return labels.get(name);
  }
}
