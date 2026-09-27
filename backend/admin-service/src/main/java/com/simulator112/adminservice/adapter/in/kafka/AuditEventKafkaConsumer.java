package com.simulator112.adminservice.adapter.in.kafka;

import com.simulator112.adminservice.adapter.in.kafka.dto.DomainAuditEventMessage;
import com.simulator112.adminservice.adapter.in.kafka.dto.HttpAuditEventMessage;
import com.simulator112.adminservice.application.port.in.RecordAuditEventUseCase;
import com.simulator112.adminservice.domain.model.AuditLogEntry;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AuditEventKafkaConsumer {

  private final RecordAuditEventUseCase recordAuditEvent;

  @KafkaListener(topics = "audit.http.requests", containerFactory = "httpAuditListenerFactory")
  public void onHttpAuditEvent(HttpAuditEventMessage event) {
    if (event == null) return;
    AuditLogEntry entry = new AuditLogEntry();
    entry.setId(UUID.randomUUID());
    entry.setOccurredAt(event.getTimestamp() != null ? event.getTimestamp() : Instant.now());
    entry.setActorUserId(parseUuid(event.getUserId()));
    entry.setActorEmail(event.getEmail());
    entry.setActorRole(event.getRole());
    entry.setAction("HTTP_REQUEST");
    entry.setResourceType(null);
    entry.setResourceId(event.getPath());
    entry.setIpAddress(event.getIp());
    entry.setDetails(Map.of("method", event.getMethod(), "path", event.getPath(), "status", event.getStatus()));
    entry.setSourceService("api-gateway");
    recordAuditEvent.record(entry);
  }

  @KafkaListener(topics = "audit.domain.events", containerFactory = "domainAuditListenerFactory")
  public void onDomainAuditEvent(DomainAuditEventMessage event) {
    if (event == null) return;
    AuditLogEntry entry = new AuditLogEntry();
    entry.setId(UUID.randomUUID());
    entry.setOccurredAt(event.getTimestamp() != null ? event.getTimestamp() : Instant.now());
    entry.setActorUserId(parseUuid(event.getActorUserId()));
    entry.setActorEmail(event.getActorEmail());
    entry.setActorRole(event.getActorRole());
    entry.setAction(event.getAction());
    entry.setResourceType(event.getResourceType());
    entry.setResourceId(event.getResourceId());
    entry.setDetails(event.getDetails());
    entry.setSourceService(event.getSourceService());
    recordAuditEvent.record(entry);
  }

  private static UUID parseUuid(String value) {
    try {
      return value == null ? null : UUID.fromString(value);
    } catch (IllegalArgumentException e) {
      return null;
    }
  }
}
