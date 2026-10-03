package com.learning.foodagent.model;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest(properties = "food-agent.model.provider=fake")
@AutoConfigureMockMvc
class ModelControllerIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void reportsConfiguredModelHealth() throws Exception {
		mockMvc.perform(get("/api/v1/model/health"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.status").value("UP"))
				.andExpect(jsonPath("$.provider").value("fake"))
				.andExpect(jsonPath("$.detail").value("READY"));
	}

	@Test
	void returnsValidatedStructuredResponse() throws Exception {
		mockMvc.perform(post("/api/v1/model/generate")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{"prompt":"Find vegetarian dosa"}
							"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.category").value("FOOD_REQUEST"))
				.andExpect(jsonPath("$.message").value("Deterministic fake response for: Find vegetarian dosa"));
	}

	@Test
	void rejectsInvalidPromptBeforeCallingModel() throws Exception {
		mockMvc.perform(post("/api/v1/model/generate")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{"prompt":""}
							"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
	}
}
