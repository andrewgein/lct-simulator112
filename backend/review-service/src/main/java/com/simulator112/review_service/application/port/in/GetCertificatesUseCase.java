package com.simulator112.review_service.application.port.in;

import com.simulator112.review_service.domain.model.Certificate;

import java.util.List;
import java.util.UUID;

public interface GetCertificatesUseCase {
    List<Certificate> getByUserId(UUID userId);

    Certificate getById(UUID id, UUID userId);
}
