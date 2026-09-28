package com.simulator112.auth.domain.exception;

public class UserDeletionConflictException extends RuntimeException {
    public UserDeletionConflictException(String message) {
        super(message);
    }
}
