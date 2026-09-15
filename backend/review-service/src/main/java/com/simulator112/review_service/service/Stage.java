package com.simulator112.review_service.service;

import com.simulator112.context.grpc.contract.FullContext;
import com.simulator112.review_service.model.entity.CriterionResult;
import lombok.RequiredArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@RequiredArgsConstructor
public
class Stage {
    private final String name;
    private final List<Criterion> criteria;

    public int getMaxScore() {
        return criteria.stream().mapToInt(Criterion::getMaxScore).sum();
    }

    public List<CriterionResult> evaluate(FullContext context) {
        return criteria.stream()
                .flatMap(criterion -> criterion.evaluate(context).stream())
                .collect(Collectors.toList());
    }
}