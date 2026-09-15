package com.simulator112.incident.mapper.embeddable;

import com.simulator112.incident.dto.request.embeddable.AddressRequest;
import com.simulator112.incident.dto.view.embeddable.AddressView;
import com.simulator112.incident.model.embeddable.Address;
import org.springframework.stereotype.Component;

@Component
public class AddressMapper {

    public Address toEntity(AddressRequest request) {

        Address address = new Address();

        address.setCity(request.city());
        address.setStreet(request.street());
        address.setHouse(request.house());
        address.setBuilding(request.building());
        address.setApartment(request.apartment());
        address.setFloor(request.floor());

        return address;
    }

    public AddressView toView(Address address) {
        return new AddressView(
                address.getCity(),
                address.getStreet(),
                address.getHouse(),
                address.getBuilding(),
                address.getApartment(),
                address.getFloor()
        );
    }
}
