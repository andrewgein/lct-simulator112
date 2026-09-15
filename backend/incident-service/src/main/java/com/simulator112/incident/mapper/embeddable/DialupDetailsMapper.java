package com.simulator112.incident.mapper.embeddable;

import com.simulator112.incident.dto.request.embeddable.DialupDetailsRequest;
import com.simulator112.incident.dto.view.embeddable.DialupDetailsView;
import com.simulator112.incident.model.embeddable.DialupDetails;
import org.springframework.stereotype.Component;

@Component
public class DialupDetailsMapper {

    public DialupDetails toEntity(DialupDetailsRequest request) {
        DialupDetails details = new DialupDetails();
        details.setGender(request.gender());
        details.setKnownFacts(request.knownFacts());
        details.setHiddenFacts(request.hiddenFacts());
        details.setAiContext(request.aiContext());
        details.setEmotionalState(request.emotionalState());
        return details;
    }

    public DialupDetailsView toView(DialupDetails details) {
        return new DialupDetailsView(
                details.getGender(),
                details.getKnownFacts().stream().toList(),
                details.getHiddenFacts().stream().toList(),
                details.getAiContext(),
                details.getEmotionalState()
        );
    }
}
