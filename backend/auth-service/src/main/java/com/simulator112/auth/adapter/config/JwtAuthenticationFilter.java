package com.simulator112.auth.adapter.config;

import com.simulator112.auth.adapter.out.security.JwtService;
import com.simulator112.auth.application.port.in.ValidateSessionUseCase;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.List;
import java.util.UUID;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.NonNull;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtService jwtService;
  private final ValidateSessionUseCase accounts;

  @Override
  protected void doFilterInternal(
      @NonNull HttpServletRequest request,
      @NonNull HttpServletResponse response,
      @NonNull FilterChain chain)
      throws ServletException, IOException {
    String header = request.getHeader("Authorization");
    if (header == null || !header.startsWith("Bearer ")) {
      chain.doFilter(request, response);
      return;
    }
    try {
      String token = header.substring(7);
      String role = jwtService.extractRole(token);
      UUID userId = jwtService.extractUserId(token);
      if (!accounts.isSessionValid(userId, role)) {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        return;
      }
      var auth =
          new UsernamePasswordAuthenticationToken(
              userId, null, List.of(new SimpleGrantedAuthority(role)));
      SecurityContextHolder.getContext().setAuthentication(auth);
    } catch (io.jsonwebtoken.JwtException | IllegalArgumentException e) {
      log.warn("Invalid JWT token: {}", e.getMessage());
    } catch (Exception e) {
      log.error("Cannot validate user session", e);
      response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
      return;
    }
    chain.doFilter(request, response);
  }
}
