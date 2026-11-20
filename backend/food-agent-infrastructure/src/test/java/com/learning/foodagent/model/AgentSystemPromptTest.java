package com.learning.foodagent.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;

class AgentSystemPromptTest {

	@Test
	void loadsPromptFromResource() {
		var prompt = new AgentSystemPrompt(new ByteArrayResource("  constrained prompt  ".getBytes()));

		assertEquals("constrained prompt", prompt.text());
	}

	@Test
	void rejectsBlankPrompt() {
		assertThrows(IllegalStateException.class,
				() -> new AgentSystemPrompt(new ByteArrayResource("   ".getBytes())));
	}
}
