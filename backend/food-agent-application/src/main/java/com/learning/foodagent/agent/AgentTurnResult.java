package com.learning.foodagent.agent;

import java.util.List;
import java.util.Objects;
import java.util.UUID;

import com.learning.foodagent.domain.discovery.RestaurantSearchResult;

public record AgentTurnResult(
		UUID turnId,
		Outcome outcome,
		String message,
		List<RestaurantSearchResult> restaurantSearches,
		List<AgentTraceEvent> trace) {

	public AgentTurnResult {
		turnId = Objects.requireNonNull(turnId, "turnId must not be null");
		outcome = Objects.requireNonNull(outcome, "outcome must not be null");
		message = Objects.requireNonNull(message, "message must not be null").strip();
		restaurantSearches = List.copyOf(Objects.requireNonNull(
				restaurantSearches, "restaurantSearches must not be null"));
		trace = List.copyOf(Objects.requireNonNull(trace, "trace must not be null"));
		if (message.isEmpty()) {
			throw new IllegalArgumentException("message must not be blank");
		}
	}

	public enum Outcome {
		COMPLETED,
		CLARIFICATION_REQUIRED,
		UNSUPPORTED
	}
}
