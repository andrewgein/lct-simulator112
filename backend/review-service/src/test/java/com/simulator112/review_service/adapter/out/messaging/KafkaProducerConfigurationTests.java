package com.simulator112.review_service.adapter.out.messaging;

import com.simulator112.review_service.adapter.out.messaging.dto.ReviewCommentCreatedEvent;
import org.apache.kafka.clients.producer.Producer;
import org.junit.jupiter.api.Test;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JacksonJsonSerializer;
import org.springframework.test.util.ReflectionTestUtils;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaProducerConfigurationTests {
    @Test
    void createsProducerWithJsonSerializer() {
        var configuration = new KafkaProducerConfiguration();
        ReflectionTestUtils.setField(configuration, "bootstrapServers", "localhost:9092");
        ProducerFactory<String, Object> factory = configuration.kafkaProducerFactory();

        Producer<String, Object> producer = factory.createProducer();

        assertThat(producer).isNotNull();
        producer.close();
    }

    @Test
    void serializesEventWithInstant() {
        UUID eventId = UUID.randomUUID();
        var event = new ReviewCommentCreatedEvent(eventId, UUID.randomUUID(), UUID.randomUUID(),
                UUID.randomUUID(), "Комментарий", Instant.parse("2026-09-25T12:00:00Z"));

        try (var serializer = new JacksonJsonSerializer<Object>()) {
            String json = new String(serializer.serialize("review.comment.created", event), StandardCharsets.UTF_8);

            assertThat(json).contains(eventId.toString()).contains("Комментарий").contains("2026-09-25T12:00:00Z");
        }
    }
}
