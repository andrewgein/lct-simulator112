package com.simulator112.incident.mapper.embeddable;

import com.simulator112.incident.dto.request.embeddable.DispatcherCriteriaRequest;
import com.simulator112.incident.dto.view.embeddable.DispatcherCriteriaView;
import com.simulator112.incident.model.embeddable.DispatcherCriteria;
import org.springframework.stereotype.Component;

@Component
public class DispatcherCriteriaMapper {

    public DispatcherCriteria toEntity(DispatcherCriteriaRequest request) {

        DispatcherCriteria criteria = new DispatcherCriteria();

        criteria.setRequiredQuestions(request.requiredQuestions());
        criteria.setExpectedActions(request.expectedActions());
        criteria.setCriticalMistakes(request.criticalMistakes());

        return criteria;
    }

    public DispatcherCriteriaView toView(DispatcherCriteria criteria) {
        return new DispatcherCriteriaView(
                criteria.getRequiredQuestions(),
                criteria.getExpectedActions(),
                criteria.getCriticalMistakes()
        );
    }
}
