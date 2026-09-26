package com.simulator112.profileservice.adapter.in.grpc;

import com.simulator112.profileservice.domain.model.TrainingTrack;

final class UserProfileGrpcMapper {

    private UserProfileGrpcMapper() {
    }

    static com.simulator112.profile.grpc.contract.ProfessionalProfile toProto(
            com.simulator112.profileservice.domain.model.ProfessionalProfile profile) {
        com.simulator112.profile.grpc.contract.ProfessionalProfile.Builder builder =
                com.simulator112.profile.grpc.contract.ProfessionalProfile.newBuilder()
                        .setTrainingTrack(toProto(profile.trainingTrack()));

        if (profile.ddsService() != null) {
            builder.setDdsServiceCode(profile.ddsService());
        }
        return builder.build();
    }

    private static com.simulator112.profile.grpc.contract.TrainingTrack toProto(
            TrainingTrack trainingTrack) {
        return switch (trainingTrack) {
            case SYSTEM_112 -> com.simulator112.profile.grpc.contract.TrainingTrack.TRAINING_TRACK_SYSTEM_112;
            case DDS -> com.simulator112.profile.grpc.contract.TrainingTrack.TRAINING_TRACK_DDS;
        };
    }

}
