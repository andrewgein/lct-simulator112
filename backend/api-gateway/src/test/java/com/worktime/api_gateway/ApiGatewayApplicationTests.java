package com.worktime.api_gateway;

import com.simulator112.api_gateway.ApiGatewayApplication;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(classes = ApiGatewayApplication.class, properties = "jwt.public-key=classpath:public.pem")
class ApiGatewayApplicationTests {

	@Test
	void contextLoads() {
	}

}
