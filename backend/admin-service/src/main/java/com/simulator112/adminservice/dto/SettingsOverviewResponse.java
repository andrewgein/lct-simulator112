package com.simulator112.adminservice.dto;

import java.util.Map;

/** Read-only snapshot of the currently-running config across services - no write endpoint.
 * To change a value: edit prod/.env on the server and redeploy the affected service from
 * /admin/ops (see OpsController) - deliberately not editable live from this API. */
public record SettingsOverviewResponse(
    Map<String, Object> gateway, Map<String, Object> auth, Map<String, Object> dialog) {}
