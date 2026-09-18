package com.simulator112.api_gateway.filter;

import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.springframework.cloud.gateway.filter.GatewayFilter;
import org.springframework.cloud.gateway.filter.factory.AbstractGatewayFilterFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class RoleAuthorizationFilterFactory
    extends AbstractGatewayFilterFactory<RoleAuthorizationFilterFactory.Config> {

  public RoleAuthorizationFilterFactory() {
    super(Config.class);
  }

  @Override
  public String name() {
    return "RoleAuthorization";
  }

  @Override
  public GatewayFilter apply(Config config) {
    return (exchange, chain) -> {
      String userRole = exchange.getRequest().getHeaders().getFirst("X-User-Role");

      if (userRole == null || !config.getRoles().contains(userRole)) {
        exchange.getResponse().setStatusCode(HttpStatus.FORBIDDEN);
        return exchange.getResponse().setComplete();
      }

      return chain.filter(exchange);
    };
  }

  @Getter
  @Setter
  public static class Config {
    private Set<String> roles;

    public boolean hasRole(String role) {
      return roles != null && roles.contains(role);
    }
  }
}
