package com.simulator112.profileservice.adapter.in.grpc;

import com.simulator112.profileservice.domain.model.DdsService;
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
            builder.setDdsService(toProto(profile.ddsService()));
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

    private static com.simulator112.profile.grpc.contract.DdsService toProto(DdsService service) {
        return switch (service) {
            case FIRE -> com.simulator112.profile.grpc.contract.DdsService.DDS_SERVICE_FIRE;
            case POLICE -> com.simulator112.profile.grpc.contract.DdsService.DDS_SERVICE_POLICE;
            case AMBULANCE -> com.simulator112.profile.grpc.contract.DdsService.DDS_SERVICE_AMBULANCE;
            case GAS -> com.simulator112.profile.grpc.contract.DdsService.DDS_SERVICE_GAS;
            case ANTI_TERROR -> com.simulator112.profile.grpc.contract.DdsService.DDS_SERVICE_ANTI_TERROR;
        };
    }
}
