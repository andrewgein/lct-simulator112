package com.simulator112.userservice.exception;

import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class ProfileNotFoundException extends RuntimeException {

  public ProfileNotFoundException(UUID userId) {
    super("Пользователь с id " + userId + " не найден");
  }
}
