package com.simulator112.incident.mapper.embeddable;

import com.simulator112.incident.dto.request.embeddable.ApplicantRequest;
import com.simulator112.incident.dto.request.embeddable.VictimRequest;
import com.simulator112.incident.dto.view.embeddable.ApplicantView;
import com.simulator112.incident.model.embeddable.Applicant;
import org.springframework.stereotype.Component;

@Component
public class ApplicantMapper {

    public Applicant toEntity(ApplicantRequest request) {

        Applicant applicant = new Applicant();

        applicant.setFirstName(request.firstName());
        applicant.setLastName(request.lastName());
        applicant.setMiddleName(request.middleName());
        applicant.setAge(request.age());
        applicant.setPhone(request.phone());
        applicant.setContactPhone(request.contactPhone());
        applicant.setAddress(request.address());
        applicant.setAdditionalInfo(request.additionalInfo());

        return applicant;
    }

    public Applicant toEntity(VictimRequest request) {
        Applicant victim = new Applicant();
        victim.setFirstName(request.firstName());
        victim.setLastName(request.lastName());
        victim.setMiddleName(request.middleName());
        victim.setAge(request.age());
        victim.setPhone(request.phone());
        victim.setContactPhone(request.contactPhone());
        victim.setAddress(request.address());
        victim.setAdditionalInfo(request.additionalInfo());
        return victim;
    }

    public ApplicantView toView(Applicant applicant) {
        return new ApplicantView(
                applicant.getFirstName(),
                applicant.getLastName(),
                applicant.getMiddleName(),
                applicant.getAge(),
                applicant.getPhone(),
                applicant.getContactPhone(),
                applicant.getAddress(),
                applicant.getAdditionalInfo()
        );
    }
}
