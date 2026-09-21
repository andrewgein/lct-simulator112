package com.simulator112.incident.domain.common;

public record Address(
        String city,
        String street,
        String house,
        String building,
        String apartment,
        Integer floor) {
}
