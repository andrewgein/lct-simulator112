package com.simulator112.adminservice.application.port.out;

import java.util.Map;

public interface ServiceSettingsPort {
  Map<String, Object> gatewaySettings();

  Map<String, Object> authSettings();

  Map<String, Object> dialogSettings();
}
