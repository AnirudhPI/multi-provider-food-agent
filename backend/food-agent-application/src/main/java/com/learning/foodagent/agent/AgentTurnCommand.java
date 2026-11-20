package com.learning.foodagent.agent;

import java.util.Objects;

import com.learning.foodagent.domain.discovery.GeoCoordinates;

public record AgentTurnCommand(String message, GeoCoordinates location) {

	public AgentTurnCommand {
		message = Objects.requireNonNull(message, "message must not be null").strip();
		if (message.isEmpty()) {
			throw new IllegalArgumentException("message must not be blank");
		}
		if (message.length() > 2_000) {
			throw new IllegalArgumentException("message must not exceed 2000 characters");
		}
	}
}
