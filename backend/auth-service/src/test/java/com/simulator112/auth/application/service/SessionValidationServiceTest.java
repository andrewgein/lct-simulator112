package com.simulator112.auth.application.service;

import com.simulator112.auth.application.port.out.AccountStore;
import com.simulator112.auth.domain.model.Account;
import com.simulator112.auth.domain.model.Role;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class SessionValidationServiceTest {
    @Test
    void onlyExistingVerifiedAccountWithCurrentRoleHasValidSession() {
        var accounts = mock(AccountStore.class);
        var service = new SessionValidationService(accounts);
        var id = UUID.randomUUID();
        when(accounts.findById(id)).thenReturn(Optional.empty());
        assertThat(service.isSessionValid(id, "ADMIN")).isFalse();
        when(accounts.findById(id)).thenReturn(Optional.of(new Account(id, "test@example.com", "hash", Role.STUDENT, true, null)));
        assertThat(service.isSessionValid(id, "ADMIN")).isFalse();
        assertThat(service.isSessionValid(id, "STUDENT")).isTrue();
        when(accounts.findById(id)).thenReturn(Optional.of(new Account(id, "test@example.com", "hash", Role.STUDENT, false, null)));
        assertThat(service.isSessionValid(id, "STUDENT")).isFalse();
    }
}
