package com.learning.foodagent.agent;

import java.time.Instant;
import java.util.Objects;
import java.util.UUID;

public record AgentTraceEvent(UUID turnId, Type type, int step, String toolName, Instant occurredAt) {

	public AgentTraceEvent {
		turnId = Objects.requireNonNull(turnId, "turnId must not be null");
		type = Objects.requireNonNull(type, "type must not be null");
		if (step < 0) {
			throw new IllegalArgumentException("step must not be negative");
		}
		toolName = normalize(toolName);
		occurredAt = Objects.requireNonNull(occurredAt, "occurredAt must not be null");
	}

	private static String normalize(String value) {
		if (value == null) {
			return null;
		}
		String normalized = value.strip();
		return normalized.isEmpty() ? null : normalized;
	}

	public enum Type {
		TURN_STARTED,
		MODEL_REQUESTED,
		TOOL_STARTED,
		TOOL_COMPLETED,
		TOOL_FAILED,
		CLARIFICATION_REQUIRED,
		UNSUPPORTED,
		TURN_COMPLETED
	}
}
