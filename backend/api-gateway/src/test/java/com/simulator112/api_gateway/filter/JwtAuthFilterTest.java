package com.simulator112.api_gateway.filter;

import io.jsonwebtoken.Jwts;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.time.Duration;
import java.util.Date;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;
import static org.assertj.core.api.Assertions.assertThat;

class JwtAuthFilterTest {
    private static KeyPair keys;

    @BeforeAll
    static void generateKeys() throws Exception {
        var generator = KeyPairGenerator.getInstance("RSA");
        generator.initialize(2048);
        keys = generator.generateKeyPair();
    }

    @Test
    void revokedAccountCannotUseOtherwiseValidJwt() {
        assertSessionStatus(HttpStatus.UNAUTHORIZED, HttpStatus.UNAUTHORIZED, false);
    }

    @Test
    void existingAccountCanAccessService() {
        assertSessionStatus(HttpStatus.NO_CONTENT, null, true);
    }

    @Test
    void authServiceFailureDoesNotAllowAccess() {
        assertSessionStatus(HttpStatus.SERVICE_UNAVAILABLE, HttpStatus.SERVICE_UNAVAILABLE, false);
    }

    private void assertSessionStatus(HttpStatus authStatus, HttpStatus resultStatus, boolean allowed) {
        String token = Jwts.builder().subject(UUID.randomUUID().toString()).claim("email", "test@example.com")
                .claim("role", "STUDENT").expiration(new Date(System.currentTimeMillis() + 60000))
                .signWith(keys.getPrivate()).compact();
        var client = WebClient.builder().exchangeFunction(request -> {
            assertThat(request.url().getPath()).isEqualTo("/api/v1/auth/session");
            assertThat(request.headers().getFirst(HttpHeaders.AUTHORIZATION)).isEqualTo("Bearer " + token);
            return Mono.just(ClientResponse.create(authStatus).build());
        });
        var filter = new JwtAuthFilter((RSAPublicKey) keys.getPublic(), client, "http://auth-service:8081");
        var exchange = MockServerWebExchange.from(MockServerHttpRequest.get("/api/v1/review/test")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
        var called = new AtomicBoolean();
        filter.apply(new Object()).filter(exchange, validated -> {
            called.set(true);
            assertThat(validated.getRequest().getHeaders().getFirst("X-User-Role")).isEqualTo("STUDENT");
            return Mono.empty();
        }).block(Duration.ofSeconds(5));
        assertThat(called.get()).isEqualTo(allowed);
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(resultStatus);
    }
}
