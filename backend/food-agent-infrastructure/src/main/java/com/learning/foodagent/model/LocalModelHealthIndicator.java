package com.learning.foodagent.model;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
final class LocalModelHealthIndicator implements HealthIndicator {

	private final LocalModelGateway modelGateway;

	LocalModelHealthIndicator(LocalModelGateway modelGateway) {
		this.modelGateway = modelGateway;
	}

	@Override
	public Health health() {
		ModelHealth modelHealth = modelGateway.health();
		Health.Builder health = modelHealth.status() == ModelHealth.Status.UP ? Health.up() : Health.down();

		return health
				.withDetail("provider", modelHealth.provider())
				.withDetail("model", modelHealth.model())
				.withDetail("detail", modelHealth.detail())
				.withDetail("checkedAt", modelHealth.checkedAt())
				.build();
	}
}
