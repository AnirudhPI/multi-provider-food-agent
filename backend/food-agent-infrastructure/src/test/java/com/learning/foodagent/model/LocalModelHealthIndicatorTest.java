package com.learning.foodagent.model;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;

import org.junit.jupiter.api.Test;
import org.springframework.boot.health.contributor.Status;

class LocalModelHealthIndicatorTest {

	@Test
	void reportsGatewayHealthToActuator() {
		LocalModelGateway gateway = new LocalModelGateway() {
			@Override
			public ModelHealth health() {
				return new ModelHealth(ModelHealth.Status.DOWN, "ollama", "qwen3:8b", "MODEL_NOT_INSTALLED", Instant.EPOCH);
			}

			@Override
			public ModelResult generate(ModelPrompt prompt) {
				throw new UnsupportedOperationException();
			}
		};
		var indicator = new LocalModelHealthIndicator(gateway);

		var health = indicator.health();

		assertThat(health.getStatus()).isEqualTo(Status.DOWN);
		assertThat(health.getDetails())
				.containsEntry("provider", "ollama")
				.containsEntry("model", "qwen3:8b")
				.containsEntry("detail", "MODEL_NOT_INSTALLED");
	}
}
