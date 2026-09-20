package com.simulator112.profileservice.adapter.in.grpc;

import com.simulator112.profile.grpc.contract.GetProfessionalProfileRequest;
import com.simulator112.profile.grpc.contract.ProfessionalProfile;
import com.simulator112.profile.grpc.contract.UserProfileServiceGrpc;
import com.simulator112.profileservice.application.port.in.GetProfessionalProfileUseCase;
import com.simulator112.profileservice.domain.exception.ProfessionalProfileNotAssignedException;
import com.simulator112.profileservice.domain.exception.ProfileNotFoundException;
import io.grpc.Status;
import io.grpc.stub.StreamObserver;
import lombok.RequiredArgsConstructor;
import net.devh.boot.grpc.server.service.GrpcService;

import java.util.UUID;

@GrpcService
@RequiredArgsConstructor
public class UserProfileGrpcController extends UserProfileServiceGrpc.UserProfileServiceImplBase {

    private final GetProfessionalProfileUseCase professionalProfiles;

    @Override
    public void getProfessionalProfile(
            GetProfessionalProfileRequest request,
            StreamObserver<ProfessionalProfile> responseObserver) {
        try {
            UUID userId = UUID.fromString(request.getUserId());
            ProfessionalProfile response = UserProfileGrpcMapper.toProto(
                    professionalProfiles.getProfessionalProfile(userId));
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } catch (IllegalArgumentException exception) {
            responseObserver.onError(Status.INVALID_ARGUMENT
                    .withDescription("Некорректный user_id")
                    .withCause(exception)
                    .asRuntimeException());
        } catch (ProfileNotFoundException exception) {
            responseObserver.onError(Status.NOT_FOUND
                    .withDescription(exception.getMessage())
                    .asRuntimeException());
        } catch (ProfessionalProfileNotAssignedException exception) {
            responseObserver.onError(Status.FAILED_PRECONDITION
                    .withDescription(exception.getMessage())
                    .asRuntimeException());
        }
    }
}
