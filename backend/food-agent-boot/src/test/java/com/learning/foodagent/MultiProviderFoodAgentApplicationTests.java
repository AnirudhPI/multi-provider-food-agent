package com.learning.foodagent;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = "food-agent.model.provider=fake")
class MultiProviderFoodAgentApplicationTests {

	@Test
	void contextLoads() {
	}

}
