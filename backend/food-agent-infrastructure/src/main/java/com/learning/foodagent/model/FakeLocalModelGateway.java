package com.learning.foodagent.model;

import java.time.Instant;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "food-agent.model.provider", havingValue = "fake")
final class FakeLocalModelGateway implements LocalModelGateway {

	private final ModelProperties properties;

	FakeLocalModelGateway(ModelProperties properties) {
		this.properties = properties;
	}

	@Override
	public ModelHealth health() {
		return new ModelHealth(ModelHealth.Status.UP, "fake", properties.model(), "READY", Instant.now());
	}

	@Override
	public ModelResult generate(ModelPrompt prompt) {
		return new ModelResult(
				ModelResult.Category.FOOD_REQUEST,
				"Deterministic fake response for: " + prompt.prompt());
	}
}
