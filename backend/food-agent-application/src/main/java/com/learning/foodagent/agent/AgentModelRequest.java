package com.learning.foodagent.agent;

import java.util.List;
import java.util.Objects;

public record AgentModelRequest(
		String userMessage,
		boolean locationAvailable,
		List<ToolObservation> observations) {

	public AgentModelRequest {
		userMessage = Objects.requireNonNull(userMessage, "userMessage must not be null").strip();
		if (userMessage.isEmpty()) {
			throw new IllegalArgumentException("userMessage must not be blank");
		}
		observations = List.copyOf(Objects.requireNonNull(observations, "observations must not be null"));
	}
}
