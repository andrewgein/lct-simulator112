package com.simulator112.contextmanager.model.embeddable;

import com.simulator112.contextmanager.model.enums.SpeakerType;
import jakarta.persistence.Column;
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
public class Phrase {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private SpeakerType speaker;

    @Column(nullable = false, columnDefinition = "text")
    private String text;
}
