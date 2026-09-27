package com.simulator112.auth.adapter.config;

import com.simulator112.auth.adapter.out.security.JwtService;

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
      var auth =
          new UsernamePasswordAuthenticationToken(
              userId, null, List.of(new SimpleGrantedAuthority(role)));
      SecurityContextHolder.getContext().setAuthentication(auth);
    } catch (Exception e) {
      log.warn("Invalid JWT token: {}", e.getMessage());
    }
    chain.doFilter(request, response);
  }
}
