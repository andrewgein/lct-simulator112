package com.simulator112.auth.application.port.out;

import com.simulator112.auth.domain.model.Account;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface AccountStore {
    Optional<Account> findByEmail(String email);
    Optional<Account> findById(UUID userId);
    boolean anyVerified();
    List<Account> findAll();
    Account save(Account account);
}
