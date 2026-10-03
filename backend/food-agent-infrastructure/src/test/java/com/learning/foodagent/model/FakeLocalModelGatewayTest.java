package com.learning.foodagent.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.URI;
import java.time.Duration;

import org.junit.jupiter.api.Test;

class FakeLocalModelGatewayTest {

	private final FakeLocalModelGateway gateway = new FakeLocalModelGateway(new ModelProperties(
			"fake", URI.create("http://localhost:11434"), "test-model", Duration.ofSeconds(1)));

	@Test
	void isDeterministicAndHealthy() {
		ModelHealth health = gateway.health();
		ModelResult response = gateway.generate(new ModelPrompt("Find dosa"));

		assertThat(health.status()).isEqualTo(ModelHealth.Status.UP);
		assertThat(health.provider()).isEqualTo("fake");
		assertThat(health.detail()).isEqualTo("READY");
		assertThat(response.category()).isEqualTo(ModelResult.Category.FOOD_REQUEST);
		assertThat(response.message()).isEqualTo("Deterministic fake response for: Find dosa");
	}
}
