package com.simulator112.adminservice.adapter.out.github;

import com.fasterxml.jackson.databind.JsonNode;
import com.simulator112.adminservice.application.port.out.CiWorkflowPort;
import com.simulator112.adminservice.domain.model.LatestRun;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Slf4j
@Component
public class GithubActionsAdapter implements CiWorkflowPort {

  private final RestClient restClient;
  private final String repository;

  public GithubActionsAdapter(
      @Value("${github.actions-token}") String token, @Value("${github.repository}") String repository) {
    this.repository = repository;
    this.restClient =
        RestClient.builder()
            .baseUrl("https://api.github.com")
            .defaultHeader("Authorization", "Bearer " + token)
            .defaultHeader("Accept", "application/vnd.github+json")
            .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
            .build();
  }

  @Override
  public void dispatch(String workflowFile, Map<String, String> inputs) {
    restClient
        .post()
        .uri("/repos/" + repository + "/actions/workflows/" + workflowFile + "/dispatches")
        .body(Map.of("ref", "main", "inputs", inputs == null ? Map.of() : inputs))
        .retrieve()
        .toBodilessEntity();
    log.info("Dispatched workflow {} for {} with inputs {}", workflowFile, repository, inputs);
  }

  @Override
  public LatestRun latestRun(String workflowFile) {
    try {
      JsonNode response =
          restClient
              .get()
              .uri("/repos/" + repository + "/actions/workflows/" + workflowFile + "/runs?per_page=1")
              .retrieve()
              .body(JsonNode.class);
      JsonNode run = response == null ? null : response.path("workflow_runs").path(0);
      if (run == null || run.isMissingNode()) return null;
      return new LatestRun(
          run.path("status").asText(null), run.path("conclusion").asText(null), run.path("html_url").asText(null));
    } catch (Exception e) {
      log.warn("Failed to fetch latest run for {}: {}", workflowFile, e.getMessage());
      return null;
    }
  }
}
