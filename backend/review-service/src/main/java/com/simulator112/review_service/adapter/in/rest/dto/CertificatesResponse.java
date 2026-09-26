package com.simulator112.review_service.adapter.in.rest.dto;

import java.util.List;

public record CertificatesResponse(List<CertificateResponse> certificates) {
    public CertificatesResponse {
        certificates = List.copyOf(certificates);
    }
}
