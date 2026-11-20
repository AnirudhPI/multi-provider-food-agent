package com.learning.foodagent.agent;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

@Validated
@ConfigurationProperties("food-agent.agent")
public record AgentProperties(
		@Min(1) @Max(10) int maximumSteps,
		@Min(100) @Max(50_000) int maximumObservationCharacters,
		@Min(1) @Max(20) int maximumResultsPerTool,
		@NotNull Duration timeout) {

	public AgentProperties {
		if (timeout != null && (timeout.isZero() || timeout.isNegative())) {
			throw new IllegalArgumentException("Agent timeout must be greater than zero");
		}
	}
}
