package com.simulator112.api_gateway.filter;

import com.simulator112.api_gateway.dto.HttpAuditEvent;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Slf4j
@Component
public class AuditLoggingFilter implements GlobalFilter, Ordered {

  private static final String TOPIC = "audit.http.requests";
  private static final Set<HttpMethod> MUTATING_METHODS =
      Set.of(HttpMethod.POST, HttpMethod.PUT, HttpMethod.PATCH, HttpMethod.DELETE);

  private final RSAPublicKey publicKey;
  private final KafkaTemplate<String, Object> kafkaTemplate;
  private final List<String> loggedPathPrefixes;

  public AuditLoggingFilter(
      RSAPublicKey publicKey,
      KafkaTemplate<String, Object> kafkaTemplate,
      @Value("#{'${audit.logged-path-prefixes}'.split(',')}") List<String> loggedPathPrefixes) {
    this.publicKey = publicKey;
    this.kafkaTemplate = kafkaTemplate;
    this.loggedPathPrefixes = loggedPathPrefixes;
  }

  @Override
  public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
    String path = exchange.getRequest().getPath().value();
    HttpMethod method = exchange.getRequest().getMethod();
    if (method == null || !MUTATING_METHODS.contains(method) || !isLoggedPath(path)) {
      return chain.filter(exchange);
    }
    return chain.filter(exchange).then(Mono.fromRunnable(() -> publish(exchange, path, method)));
  }

  private boolean isLoggedPath(String path) {
    return loggedPathPrefixes.stream().anyMatch(path::startsWith);
  }

  private void publish(ServerWebExchange exchange, String path, HttpMethod method) {
    try {
      Claims claims = parseClaims(exchange.getRequest().getHeaders());
      String ip =
          exchange.getRequest().getRemoteAddress() != null
              ? exchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
              : null;
      int status =
          exchange.getResponse().getStatusCode() != null
              ? exchange.getResponse().getStatusCode().value()
              : 0;
      HttpAuditEvent event =
          new HttpAuditEvent(
              claims != null ? claims.getSubject() : null,
              claims != null ? claims.get("email", String.class) : null,
              claims != null ? claims.get("role", String.class) : null,
              method.name(),
              path,
              status,
              ip,
              Instant.now());
      kafkaTemplate
          .send(TOPIC, event.getUserId(), event)
          .whenComplete(
              (result, ex) -> {
                if (ex != null) log.warn("Failed to publish audit event for {} {}: {}", method, path, ex.getMessage());
              });
    } catch (Exception e) {
      log.warn("Audit logging failed for {} {}: {}", method, path, e.getMessage());
    }
  }

  private Claims parseClaims(HttpHeaders headers) {
    String authHeader = headers.getFirst(HttpHeaders.AUTHORIZATION);
    if (authHeader == null || !authHeader.startsWith("Bearer ")) return null;
    try {
      return Jwts.parser().verifyWith(publicKey).build().parseSignedClaims(authHeader.substring(7)).getPayload();
    } catch (JwtException e) {
      return null;
    }
  }

  @Override
  public int getOrder() {

    return Ordered.LOWEST_PRECEDENCE - 1;
  }
}
