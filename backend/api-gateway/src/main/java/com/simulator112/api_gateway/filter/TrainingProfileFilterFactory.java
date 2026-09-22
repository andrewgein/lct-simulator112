package com.simulator112.api_gateway.filter;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class TrainingProfileFilterFactory extends AbstractGatewayFilterFactory<Object> {

  static final String TRAINING_TRACK_HEADER = "X-Training-Track";

  private final WebClient profileClient;

  public TrainingProfileFilterFactory(
      WebClient.Builder webClientBuilder,
      @Value("${PROFILE_SERVICE_URL:http://localhost:8082}") String profileServiceUrl) {
    super(Object.class);
    this.profileClient = webClientBuilder.baseUrl(profileServiceUrl).build();
  }

  @Override
  public String name() {
    return "TrainingProfile";
  }

  @Override
  public GatewayFilter apply(Object config) {
    return (exchange, chain) -> {
      ServerHttpRequest request = exchange.getRequest().mutate()
          .headers(headers -> headers.remove(TRAINING_TRACK_HEADER))
          .build();
      ServerWebExchange sanitizedExchange = exchange.mutate().request(request).build();

      String role = request.getHeaders().getFirst("X-User-Role");
      if ("ADMIN".equals(role)) {
        return chain.filter(sanitizedExchange);
      }

      String userId = request.getHeaders().getFirst("X-User-Id");
      if (userId == null) {
        return complete(sanitizedExchange, HttpStatus.UNAUTHORIZED);
      }

      return profileClient.get()
          .uri("/api/v1/profile")
          .header("X-User-Id", userId)
          .exchangeToMono(response -> {
            if (!response.statusCode().is2xxSuccessful()) {
              return response.releaseBody()
                  .then(complete(sanitizedExchange, response.statusCode()));
            }
            return response.bodyToMono(JsonNode.class)
                .flatMap(profile -> addProfileHeaders(sanitizedExchange, chain, profile));
          })
          .onErrorResume(error -> complete(sanitizedExchange, HttpStatus.BAD_GATEWAY));
    };
  }

  private Mono<Void> addProfileHeaders(
      ServerWebExchange exchange,
      org.springframework.cloud.gateway.filter.GatewayFilterChain chain,
      JsonNode profile) {
    String trainingTrack = profile.path("trainingTrack").asText(null);
    if (trainingTrack == null || trainingTrack.isBlank()) {
      return complete(exchange, HttpStatus.FORBIDDEN);
    }

    ServerHttpRequest request = exchange.getRequest().mutate()
        .header(TRAINING_TRACK_HEADER, trainingTrack)
        .build();
    return chain.filter(exchange.mutate().request(request).build());
  }

  private Mono<Void> complete(ServerWebExchange exchange, org.springframework.http.HttpStatusCode status) {
    exchange.getResponse().setStatusCode(status);
    return exchange.getResponse().setComplete();
  }
}
