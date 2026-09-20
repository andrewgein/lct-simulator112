package com.simulator112.profileservice;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "grpc.server.port=0")
class ProfileServiceApplicationTests {

    @Test
    void contextLoads() {
    }

}
