package com.learning.foodagent.agent;

import java.util.Map;
import java.util.Objects;

public sealed interface AgentModelAction
		permits AgentModelAction.ToolCall, AgentModelAction.FinalAnswer,
		AgentModelAction.Clarification, AgentModelAction.Unsupported {

	record ToolCall(String toolName, Map<String, String> arguments) implements AgentModelAction {

		public ToolCall {
			toolName = required(toolName, "toolName", 100);
			arguments = Map.copyOf(Objects.requireNonNull(arguments, "arguments must not be null"));
		}
	}

	record FinalAnswer(String message) implements AgentModelAction {

		public FinalAnswer {
			message = required(message, "message", 4_000);
		}
	}

	record Clarification(String message) implements AgentModelAction {

		public Clarification {
			message = required(message, "message", 1_000);
		}
	}

	record Unsupported(String message) implements AgentModelAction {

		public Unsupported {
			message = required(message, "message", 1_000);
		}
	}

	private static String required(String value, String field, int maximumLength) {
		String normalized = Objects.requireNonNull(value, field + " must not be null").strip();
		if (normalized.isEmpty()) {
			throw new IllegalArgumentException(field + " must not be blank");
		}
		if (normalized.length() > maximumLength) {
			throw new IllegalArgumentException(field + " must not exceed " + maximumLength + " characters");
		}
		return normalized;
	}
}
