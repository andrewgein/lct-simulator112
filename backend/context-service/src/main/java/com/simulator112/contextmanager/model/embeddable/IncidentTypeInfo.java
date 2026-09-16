package com.simulator112.contextmanager.model.embeddable;

import com.simulator112.contextmanager.model.enums.ServiceType;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Embeddable
public class IncidentTypeInfo {

    private String id;

    private String typeId;

    @Enumerated(EnumType.STRING)
    private ServiceType serviceType;

    private String typeName;
}
