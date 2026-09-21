package com.simulator112.classifier.application.port.in;

import com.simulator112.classifier.domain.model.RoutingResult;

import java.util.Map;

public interface ResolveRoutingUseCase {
    RoutingResult resolve(String classifierCode, Map<String, String> facts);
}
