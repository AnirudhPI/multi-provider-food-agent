package com.learning.foodagent.model;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Component;

@Component
final class AgentSystemPrompt {

	private final String text;

	AgentSystemPrompt(@Value("classpath:/prompts/agent-system-prompt.txt") Resource resource) {
		try {
			this.text = resource.getContentAsString(StandardCharsets.UTF_8).strip();
		}
		catch (IOException exception) {
			throw new IllegalStateException("Could not load the agent system prompt", exception);
		}
		if (text.isBlank()) {
			throw new IllegalStateException("Agent system prompt must not be blank");
		}
	}

	String text() {
		return text;
	}
}
