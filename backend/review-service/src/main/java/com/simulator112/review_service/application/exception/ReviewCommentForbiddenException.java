package com.simulator112.review_service.application.exception;

public class ReviewCommentForbiddenException extends RuntimeException {
    public ReviewCommentForbiddenException() {
        super("Оставлять комментарии можно только ученикам своих курсов");
    }
}
