package com.simulator112.adminservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Triggers workflow_dispatch runs on the repo's own GitHub Actions - the only way this project
 * starts/stops/updates a compose service. Deliberately does NOT touch /var/run/docker.sock:
 * everything goes through the already-reviewed CI workflows running on the self-hosted runner,
 * which already has whatever docker access it needs. admin-service itself never gets that access.
 */
@Slf4j
@Component
public class GithubActionsClient {

  private final RestClient restClient;
  private final String repository;

  public GithubActionsClient(
      @Value("${github.actions-token}") String token,
      @Value("${github.repository}") String repository) {
    this.repository = repository;
    this.restClient =
        RestClient.builder()
            .baseUrl("https://api.github.com")
            .defaultHeader("Authorization", "Bearer " + token)
            .defaultHeader("Accept", "application/vnd.github+json")
            .defaultHeader("X-GitHub-Api-Version", "2022-11-28")
            .build();
  }

  /** Fires workflow_dispatch for the given workflow file on "main", optionally with inputs. */
  public void dispatch(String workflowFile, Map<String, String> inputs) {
    restClient
        .post()
        // "repository" is "owner/repo" - substituting it into a single {repo} template variable
        // makes Spring's UriComponentsBuilder percent-encode the slash as %2F, producing a path
        // GitHub doesn't recognize (silent 404 on every call). Pre-building the path string and
        // passing it to .uri(String) skips template-variable encoding entirely; workflowFile is
        // always one of our own static ".yml" filenames, never user input, so this is safe.
        .uri("/repos/" + repository + "/actions/workflows/" + workflowFile + "/dispatches")
        .body(Map.of("ref", "main", "inputs", inputs == null ? Map.of() : inputs))
        .retrieve()
        .toBodilessEntity();
    log.info("Dispatched workflow {} for {} with inputs {}", workflowFile, repository, inputs);
  }

  /** Best-effort: latest run's status/conclusion for a workflow file, or null if unavailable. */
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
          run.path("status").asText(null),
          run.path("conclusion").asText(null),
          run.path("html_url").asText(null));
    } catch (Exception e) {
      log.warn("Failed to fetch latest run for {}: {}", workflowFile, e.getMessage());
      return null;
    }
  }

  public record LatestRun(String status, String conclusion, String htmlUrl) {}
}
