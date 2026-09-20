package com.simulator112.profileservice.application.port.in;

import com.simulator112.profileservice.domain.model.ProfessionalProfile;
import com.simulator112.profileservice.domain.model.UserProfile;

import java.util.UUID;

public interface AssignProfessionalProfileUseCase {

    UserProfile assignProfessionalProfile(UUID userId, ProfessionalProfile professionalProfile);
}
