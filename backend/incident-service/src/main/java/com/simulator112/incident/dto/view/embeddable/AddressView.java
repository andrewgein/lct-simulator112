package com.simulator112.incident.dto.view.embeddable;

public record AddressView(
        String city,
        String street,
        String house,
        String building,
        String apartment,
        Integer floor
) {
}
