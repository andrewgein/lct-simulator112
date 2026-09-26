package com.simulator112.review_service.adapter.in.rest;

import com.simulator112.review_service.adapter.in.rest.dto.CertificateResponse;
import com.simulator112.review_service.adapter.in.rest.dto.CertificatesResponse;
import com.simulator112.review_service.application.port.in.GetCertificatesUseCase;
import com.simulator112.review_service.domain.model.Certificate;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/review/certificates")
@RequiredArgsConstructor
public class CertificateRestController {
    private final GetCertificatesUseCase certificates;

    @GetMapping
    public CertificatesResponse getMine(@RequestHeader("X-User-Id") UUID userId) {
        return new CertificatesResponse(certificates.getByUserId(userId).stream()
                .map(CertificateRestController::toResponse).toList());
    }

    @GetMapping("/{id}")
    public CertificateResponse getOne(@RequestHeader("X-User-Id") UUID userId, @PathVariable UUID id) {
        return toResponse(certificates.getById(id, userId));
    }

    private static CertificateResponse toResponse(Certificate certificate) {
        return new CertificateResponse(certificate.id(), certificate.courseId(), certificate.courseTitle(),
                certificate.percent(), certificate.type().name(), certificate.issuedAt());
    }
}
