package com.simulator112.profileservice.adapter.in.web;

import com.simulator112.profileservice.domain.exception.InvalidProfessionalProfileException;
import com.simulator112.profileservice.domain.exception.ProfessionalProfileNotAssignedException;
import com.simulator112.profileservice.domain.exception.ProfileNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class UserProfileExceptionHandler {

    @ExceptionHandler(ProfileNotFoundException.class)
    ProblemDetail handleNotFound(ProfileNotFoundException exception) {
        return problem(HttpStatus.NOT_FOUND, exception);
    }

    @ExceptionHandler(InvalidProfessionalProfileException.class)
    ProblemDetail handleInvalidProfile(InvalidProfessionalProfileException exception) {
        return problem(HttpStatus.BAD_REQUEST, exception);
    }

    @ExceptionHandler(ProfessionalProfileNotAssignedException.class)
    ProblemDetail handleUnassignedProfile(ProfessionalProfileNotAssignedException exception) {
        return problem(HttpStatus.CONFLICT, exception);
    }

    private ProblemDetail problem(HttpStatus status, RuntimeException exception) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(status, exception.getMessage());
        problem.setTitle(status.getReasonPhrase());
        return problem;
    }
}
