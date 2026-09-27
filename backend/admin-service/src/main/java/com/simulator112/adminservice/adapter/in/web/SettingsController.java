package com.simulator112.adminservice.adapter.in.web;

import com.simulator112.adminservice.adapter.in.web.dto.SettingsOverviewResponse;
import com.simulator112.adminservice.application.port.out.ServiceSettingsPort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SettingsController {

  private final ServiceSettingsPort serviceSettings;

  public SettingsController(ServiceSettingsPort serviceSettings) {
    this.serviceSettings = serviceSettings;
  }

  @GetMapping("/api/v1/admin/settings/overview")
  public SettingsOverviewResponse overview() {
    return new SettingsOverviewResponse(
        serviceSettings.gatewaySettings(), serviceSettings.authSettings(), serviceSettings.dialogSettings());
  }
}
