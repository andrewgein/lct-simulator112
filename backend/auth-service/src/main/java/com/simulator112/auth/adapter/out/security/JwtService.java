package com.simulator112.auth.adapter.out.security;

import com.simulator112.auth.application.port.out.TokenIssuer;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import jakarta.annotation.PostConstruct;
import java.security.KeyFactory;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.RSAPublicKeySpec;
import java.util.Base64;
import java.util.Date;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import com.simulator112.auth.adapter.out.security.dto.UserDetails;

@Service
public class JwtService implements TokenIssuer {

  @Value("${jwt.private-key}")
  private Resource privateKeyResource;

  @Value("${jwt.access-token-expiration}")
  private long accessTokenExpiration;

  private RSAPrivateCrtKey privateKey;
  private RSAPublicKey publicKey;

  @PostConstruct
  private void init() throws Exception {
    String pem =
        new String(privateKeyResource.getInputStream().readAllBytes())
            .replace("-----BEGIN PRIVATE KEY-----", "")
            .replace("-----END PRIVATE KEY-----", "")
            .replaceAll("\\s", "");
    byte[] decoded = Base64.getDecoder().decode(pem);
    KeyFactory kf = KeyFactory.getInstance("RSA");
    privateKey = (RSAPrivateCrtKey) kf.generatePrivate(new PKCS8EncodedKeySpec(decoded));
    publicKey =
        (RSAPublicKey)
            kf.generatePublic(
                new RSAPublicKeySpec(privateKey.getModulus(), privateKey.getPublicExponent()));
  }

  public String generateAccessToken(UserDetails userDetails) {
    return Jwts.builder()
        .subject(userDetails.getUserId().toString())
        .claim("email", userDetails.getUsername())
        .claim("role", userDetails.getRole())
        .issuedAt(new Date())
        .expiration(new Date(System.currentTimeMillis() + accessTokenExpiration))
        .signWith(privateKey, Jwts.SIG.RS256)
        .compact();
  }

  public String generateRefreshToken() {
    return UUID.randomUUID().toString();
  }

  @Override
  public String issueAccess(UUID userId, String email, String role) {
    return generateAccessToken(new UserDetails(email, userId, role));
  }

  @Override
  public String issueRefresh() {
    return generateRefreshToken();
  }

  public String extractEmail(String token) {
    return extractClaim(token, claims -> claims.get("email", String.class));
  }

  public UUID extractUserId(String token) {
    return UUID.fromString(extractClaim(token, Claims::getSubject));
  }

  public String extractRole(String token) {
    return extractClaim(token, claims -> claims.get("role", String.class));
  }

  public boolean isTokenValid(String token, UserDetails userDetails) {
    final String email = extractEmail(token);
    return email.equals(userDetails.getUsername()) && !isTokenExpired(token);
  }

  private boolean isTokenExpired(String token) {
    return extractClaim(token, Claims::getExpiration).before(new Date());
  }

  private <T> T extractClaim(String token, Function<Claims, T> claimsResolver) {
    return claimsResolver.apply(extractAllClaims(token));
  }

  private Claims extractAllClaims(String token) {
    return Jwts.parser().verifyWith(publicKey).build().parseSignedClaims(token).getPayload();
  }
}
