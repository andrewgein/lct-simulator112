package com.simulator112.notification.adapter.out.persistence;

import com.simulator112.notification.domain.model.Notification;

final class NotificationPersistenceMapper {
    private NotificationPersistenceMapper() {}

    static Notification toDomain(NotificationJpaEntity entity) {
        Notification value = new Notification();
        value.setId(entity.getId());
        value.setEventId(entity.getEventId());
        value.setUserId(entity.getUserId());
        value.setType(entity.getType());
        value.setTitle(entity.getTitle());
        value.setText(entity.getText());
        value.setTargetUrl(entity.getTargetUrl());
        value.setCreatedAt(entity.getCreatedAt());
        value.setReadAt(entity.getReadAt());
        value.setEmailStatus(entity.getEmailStatus());
        value.setEmailSentAt(entity.getEmailSentAt());
        return value;
    }

    static NotificationJpaEntity toEntity(Notification value) {
        NotificationJpaEntity entity = new NotificationJpaEntity();
        entity.setId(value.getId());
        entity.setEventId(value.getEventId());
        entity.setUserId(value.getUserId());
        entity.setType(value.getType());
        entity.setTitle(value.getTitle());
        entity.setText(value.getText());
        entity.setTargetUrl(value.getTargetUrl());
        entity.setCreatedAt(value.getCreatedAt());
        entity.setReadAt(value.getReadAt());
        entity.setEmailStatus(value.getEmailStatus());
        entity.setEmailSentAt(value.getEmailSentAt());
        return entity;
    }
}
