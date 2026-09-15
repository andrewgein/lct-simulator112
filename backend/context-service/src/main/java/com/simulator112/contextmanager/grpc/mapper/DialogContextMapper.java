package com.simulator112.contextmanager.grpc.mapper;

import com.simulator112.contextmanager.model.embeddable.Phrase;
import com.simulator112.contextmanager.model.entity.DialogContextEntity;
import com.simulator112.contextmanager.model.enums.SpeakerType;
import com.simulator112.context.grpc.contract.DialogContext;

public class DialogContextMapper {
    private DialogContextMapper() {}

    public static DialogContextEntity toEntity(DialogContext proto) {
        DialogContextEntity entity = new DialogContextEntity();
        proto.getTranscriptList().forEach(phrase -> entity.getTranscript().add(new Phrase(
                SpeakerType.valueOf(phrase.getSpeaker().name()),
                phrase.getText())));
        return entity;
    }

    public static void appendToEntity(DialogContextEntity entity, DialogContext proto) {
        proto.getTranscriptList().forEach(phrase -> entity.getTranscript().add(new Phrase(
                SpeakerType.valueOf(phrase.getSpeaker().name()),
                phrase.getText())));
    }

    public static DialogContext toProto(DialogContextEntity entity) {
        DialogContext.Builder builder = DialogContext.newBuilder();
        entity.getTranscript().forEach(phrase -> builder.addTranscript(
                com.simulator112.context.grpc.contract.Phrase.newBuilder()
                        .setSpeaker(com.simulator112.context.grpc.contract.SpeakerType.valueOf(phrase.getSpeaker().name()))
                        .setText(phrase.getText() == null ? "" : phrase.getText())
                        .build()));
        return builder.build();
    }
}
