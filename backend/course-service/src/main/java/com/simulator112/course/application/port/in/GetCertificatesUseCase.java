package com.simulator112.course.application.port.in;

import com.simulator112.course.domain.model.Certificate;

import java.util.List;
import java.util.UUID;

public interface GetCertificatesUseCase {
    List<Certificate> getByUserId(UUID userId);

    Certificate getById(UUID id, UUID userId);
}
