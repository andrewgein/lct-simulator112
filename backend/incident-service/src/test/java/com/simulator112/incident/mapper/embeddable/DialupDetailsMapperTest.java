package com.simulator112.incident.mapper.embeddable;

import com.simulator112.incident.dto.view.embeddable.DialupDetailsView;
import com.simulator112.incident.model.embeddable.DialupDetails;
import com.simulator112.incident.model.enums.Gender;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class DialupDetailsMapperTest {

    private final DialupDetailsMapper mapper = new DialupDetailsMapper();

    @Test
    void copiesFactCollectionsToView() {
        List<String> knownFacts = new ArrayList<>(List.of("Известный факт"));
        List<String> hiddenFacts = new ArrayList<>(List.of("Скрытый факт"));
        DialupDetails details = DialupDetails.builder()
                .gender(Gender.WOMEN)
                .knownFacts(knownFacts)
                .hiddenFacts(hiddenFacts)
                .aiContext("Контекст")
                .emotionalState("Взволнован")
                .build();

        DialupDetailsView view = mapper.toView(details);
        knownFacts.add("Новый известный факт");
        hiddenFacts.add("Новый скрытый факт");

        assertThat(view.knownFacts()).containsExactly("Известный факт");
        assertThat(view.hiddenFacts()).containsExactly("Скрытый факт");
        assertThat(view.gender()).isEqualTo(Gender.WOMEN);
        assertThat(view.aiContext()).isEqualTo("Контекст");
        assertThat(view.emotionalState()).isEqualTo("Взволнован");
    }
}
