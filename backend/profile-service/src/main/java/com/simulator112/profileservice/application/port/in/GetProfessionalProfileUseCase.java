package com.simulator112.profileservice.application.port.in;

import com.simulator112.profileservice.domain.model.ProfessionalProfile;

import java.util.UUID;

public interface GetProfessionalProfileUseCase {

    ProfessionalProfile getProfessionalProfile(UUID userId);
}
