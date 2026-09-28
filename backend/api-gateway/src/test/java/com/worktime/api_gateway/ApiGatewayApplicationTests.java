package com.worktime.api_gateway;

import com.simulator112.api_gateway.ApiGatewayApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import java.security.interfaces.RSAPublicKey;

@SpringBootTest(classes = ApiGatewayApplication.class, properties = "jwt.public-key=classpath:public.pem")
class ApiGatewayApplicationTests {

    @MockitoBean(name = "rsaPublicKey")
    RSAPublicKey publicKey;

	@Test
	void contextLoads() {
	}

}
