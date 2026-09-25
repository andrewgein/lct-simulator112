package com.simulator112.adminservice.config;

import com.simulator112.adminservice.dto.DomainAuditEventMessage;
import com.simulator112.adminservice.dto.HttpAuditEventMessage;
import java.util.HashMap;
import java.util.Map;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.support.serializer.ErrorHandlingDeserializer;
import org.springframework.kafka.support.serializer.JsonDeserializer;

/**
 * Two topics, two payload shapes: the gateway publishes generic HTTP-level events, business
 * services publish semantically meaningful domain events. Each gets its own listener container
 * factory with a JsonDeserializer pinned to its target type (producers disable type headers, see
 * ADD_TYPE_INFO_HEADERS=false on their producer configs) so this service never needs the
 * producing service's actual Java classes on its classpath.
 */
@Configuration
public class KafkaAuditConsumerConfig {

  @Value("${spring.kafka.consumer.bootstrap-servers:${spring.kafka.bootstrap-servers}}")
  private String bootstrapServers;

  private Map<String, Object> baseConsumerProps() {
    Map<String, Object> props = new HashMap<>();
    props.put(ConsumerConfig.BOOTSTRAP_SERVERS_CONFIG, bootstrapServers);
    props.put(ConsumerConfig.GROUP_ID_CONFIG, "admin-service");
    props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
    props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
    props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ErrorHandlingDeserializer.class);
    props.put("spring.deserializer.key.delegate.class", StringDeserializer.class);
    return props;
  }

  @Bean
  public ConsumerFactory<String, HttpAuditEventMessage> httpAuditConsumerFactory() {
    Map<String, Object> props = baseConsumerProps();
    JsonDeserializer<HttpAuditEventMessage> delegate =
        new JsonDeserializer<>(HttpAuditEventMessage.class, false);
    return new DefaultKafkaConsumerFactory<>(
        props, new StringDeserializer(), new ErrorHandlingDeserializer<>(delegate));
  }

  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, HttpAuditEventMessage>
      httpAuditListenerFactory() {
    ConcurrentKafkaListenerContainerFactory<String, HttpAuditEventMessage> factory =
        new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(httpAuditConsumerFactory());
    return factory;
  }

  @Bean
  public ConsumerFactory<String, DomainAuditEventMessage> domainAuditConsumerFactory() {
    Map<String, Object> props = baseConsumerProps();
    JsonDeserializer<DomainAuditEventMessage> delegate =
        new JsonDeserializer<>(DomainAuditEventMessage.class, false);
    return new DefaultKafkaConsumerFactory<>(
        props, new StringDeserializer(), new ErrorHandlingDeserializer<>(delegate));
  }

  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, DomainAuditEventMessage>
      domainAuditListenerFactory() {
    ConcurrentKafkaListenerContainerFactory<String, DomainAuditEventMessage> factory =
        new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(domainAuditConsumerFactory());
    return factory;
  }
}
