package com.learning.foodagent.agent;

import java.time.Duration;
import java.util.Objects;

public record AgentLimits(
		int maximumSteps,
		int maximumObservationCharacters,
		int maximumResultsPerTool,
		Duration timeout) {

	public AgentLimits {
		if (maximumSteps < 1 || maximumSteps > 10) {
			throw new IllegalArgumentException("maximumSteps must be between 1 and 10");
		}
		if (maximumObservationCharacters < 100 || maximumObservationCharacters > 50_000) {
			throw new IllegalArgumentException("maximumObservationCharacters must be between 100 and 50000");
		}
		if (maximumResultsPerTool < 1 || maximumResultsPerTool > 20) {
			throw new IllegalArgumentException("maximumResultsPerTool must be between 1 and 20");
		}
		timeout = Objects.requireNonNull(timeout, "timeout must not be null");
		if (timeout.isZero() || timeout.isNegative()) {
			throw new IllegalArgumentException("timeout must be greater than zero");
		}
	}
}
