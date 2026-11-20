package com.learning.foodagent.agent;

import java.util.Objects;

public record ToolObservation(String toolName, Status status, String content) {

	public ToolObservation {
		toolName = Objects.requireNonNull(toolName, "toolName must not be null").strip();
		status = Objects.requireNonNull(status, "status must not be null");
		content = Objects.requireNonNull(content, "content must not be null").strip();
		if (toolName.isEmpty() || content.isEmpty()) {
			throw new IllegalArgumentException("toolName and content must not be blank");
		}
	}

	public enum Status {
		SUCCESS,
		FAILURE
	}
}
