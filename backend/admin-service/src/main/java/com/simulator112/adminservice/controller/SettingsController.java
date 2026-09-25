package com.simulator112.adminservice.controller;

import com.simulator112.adminservice.dto.SettingsOverviewResponse;
import com.simulator112.adminservice.service.SettingsService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SettingsController {

  private final SettingsService settingsService;

  public SettingsController(SettingsService settingsService) {
    this.settingsService = settingsService;
  }

  @GetMapping("/api/v1/admin/settings/overview")
  public SettingsOverviewResponse overview() {
    return new SettingsOverviewResponse(
        settingsService.gatewaySettings(), settingsService.authSettings(), settingsService.dialogSettings());
  }
}
