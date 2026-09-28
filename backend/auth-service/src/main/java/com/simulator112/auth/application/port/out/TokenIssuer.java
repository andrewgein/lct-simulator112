package com.simulator112.auth.application.port.out;

import java.util.UUID;

public interface TokenIssuer {
    String issueAccess(UUID userId, String email, String role);
    String issueRefresh();
}
