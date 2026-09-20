package com.simulator112.incident.client.classifier;

import com.simulator112.classifier.grpc.contract.ClassifierServiceGrpc;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
class ClassifierGrpcClientConfiguration {

  @Bean(destroyMethod = "shutdown")
  ManagedChannel classifierChannel(
      @Value("${classifier.grpc.host:localhost}") String host,
      @Value("${classifier.grpc.port:9093}") int port) {
    return ManagedChannelBuilder.forAddress(host, port).usePlaintext().build();
  }

  @Bean
  ClassifierServiceGrpc.ClassifierServiceBlockingStub classifierStub(
      ManagedChannel classifierChannel) {
    return ClassifierServiceGrpc.newBlockingStub(classifierChannel);
  }
}
