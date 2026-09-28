package com.simulator112.api_gateway.filter;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class JwtAuthFilter extends AbstractGatewayFilterFactory<Object> {

  private final RSAPublicKey publicKey;
  private final WebClient authClient;

  public JwtAuthFilter(RSAPublicKey publicKey, WebClient.Builder webClient,
                       @Value("${AUTH_SERVICE_URL:http://localhost:8081}") String authServiceUrl) {
    super(Object.class);
    this.publicKey = publicKey;
    this.authClient = webClient.baseUrl(authServiceUrl).build();
  }

  @Override
  public GatewayFilter apply(Object config) {
    return (exchange, chain) -> {
      String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);

      if (authHeader == null || !authHeader.startsWith("Bearer ")) {
        return unauthorized(exchange);
      }
      try {
        String token = authHeader.substring(7);
        Claims claims =
            Jwts.parser().verifyWith(publicKey).build().parseSignedClaims(token).getPayload();

        ServerHttpRequest mutatedRequest =
            exchange
                .getRequest()
                .mutate()
                .header("X-User-Id", claims.getSubject())
                .header("X-User-Email", claims.get("email", String.class))
                .header("X-User-Role", claims.get("role", String.class))
                .build();

        return authClient.get().uri("/api/v1/auth/session")
            .header(HttpHeaders.AUTHORIZATION, authHeader)
            .exchangeToMono(response -> response.releaseBody().thenReturn(response.statusCode().value()))
            .timeout(Duration.ofSeconds(3))
            .onErrorReturn(HttpStatus.SERVICE_UNAVAILABLE.value())
            .flatMap(status -> {
              if (status == HttpStatus.NO_CONTENT.value()) {
                return chain.filter(exchange.mutate().request(mutatedRequest).build());
              }
              if (status == 401 || status == 403) return unauthorized(exchange);
              exchange.getResponse().setStatusCode(HttpStatus.SERVICE_UNAVAILABLE);
              return exchange.getResponse().setComplete();
            });

      } catch (JwtException | IllegalArgumentException e) {
        return unauthorized(exchange);
      }
    };
  }

  private Mono<Void> unauthorized(ServerWebExchange exchange) {
    exchange.getResponse().setStatusCode(HttpStatus.UNAUTHORIZED);
    return exchange.getResponse().setComplete();
  }
}
