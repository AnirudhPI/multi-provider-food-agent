package com.learning.foodagent.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

class ModelSystemPromptTest {

	@Test
	void loadsVersionedPromptFromClasspath() {
		var prompt = new ModelSystemPrompt(new ClassPathResource("prompts/food-intent-system-prompt.txt"));

		assertThat(prompt.text())
				.contains("You classify a user's message")
				.contains("Return only data matching the supplied schema.");
	}
}
