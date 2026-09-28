package com.simulator112.auth.application.port.out;

public interface PasswordHasher {
    String hash(String password);
    boolean matches(String password, String hash);
}
