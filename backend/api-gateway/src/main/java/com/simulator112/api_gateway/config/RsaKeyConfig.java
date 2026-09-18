package com.simulator112.api_gateway.config;

import java.security.interfaces.RSAPublicKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.security.converter.RsaKeyConverters;

@Configuration
public class RsaKeyConfig {

  @Bean
  public RSAPublicKey rsaPublicKey(@Value("${jwt.public-key}") Resource pemFile) throws Exception {
    return RsaKeyConverters.x509().convert(pemFile.getInputStream());
  }
}
