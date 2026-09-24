package com.simulator112.classifier.adapter.in.web.dto;

import java.util.List;

public record RoutingFactView(
        String code,
        String label,
        RoutingFactControlType controlType,
        List<RoutingFactOptionView> options
) {
    public enum RoutingFactControlType {
        BOOLEAN,
        SINGLE_SELECT
    }
}
