package com.simulator112.review_service.adapter.in.rest.dto;

import java.util.List;

public record ReviewCommentsResponse(List<ReviewCommentResponse> comments) {
    public ReviewCommentsResponse {
        comments = List.copyOf(comments);
    }
}
