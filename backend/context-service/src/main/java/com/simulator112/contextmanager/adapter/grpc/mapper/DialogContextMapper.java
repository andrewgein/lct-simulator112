package com.simulator112.contextmanager.adapter.grpc.mapper;

import com.simulator112.context.grpc.contract.DialogContext;
import com.simulator112.contextmanager.domain.common.DialogTranscript;
import com.simulator112.contextmanager.domain.common.Phrase;
import com.simulator112.contextmanager.domain.common.SpeakerType;

public final class DialogContextMapper {
    private DialogContextMapper() {}

    public static DialogTranscript toDomain(DialogContext proto) {
        return new DialogTranscript(proto.getTranscriptList().stream()
                .map(value -> new Phrase(SpeakerType.valueOf(value.getSpeaker().name()), value.getText()))
                .toList());
    }

    public static DialogContext toProto(DialogTranscript dialog) {
        var builder = DialogContext.newBuilder();
        dialog.phrases().forEach(value -> builder.addTranscript(
                com.simulator112.context.grpc.contract.Phrase.newBuilder()
                        .setSpeaker(com.simulator112.context.grpc.contract.SpeakerType.valueOf(value.speaker().name()))
                        .setText(value.text()).build()));
        return builder.build();
    }
}
