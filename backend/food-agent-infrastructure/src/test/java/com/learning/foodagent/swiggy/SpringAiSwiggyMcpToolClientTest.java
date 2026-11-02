package com.learning.foodagent.swiggy;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import io.modelcontextprotocol.spec.McpSchema;
import tools.jackson.databind.json.JsonMapper;

class SpringAiSwiggyMcpToolClientTest {

	private final JsonMapper objectMapper = JsonMapper.builder().build();

	@Test
	void prefersStructuredContentOverHumanReadableWarning() {
		var callResult = McpSchema.CallToolResult.builder()
				.addTextContent("⚠ Human-readable provider warning")
				.structuredContent(Map.of(
						"success", true,
						"data", Map.of("restaurants", List.of(), "query", "dosa")))
				.isError(false)
				.build();

		var payload = SpringAiSwiggyMcpToolClient.payload(callResult, objectMapper);

		assertThat(payload).contains("\"success\":true")
				.contains("\"restaurants\":[]")
				.doesNotContain("⚠");
		assertThat(SpringAiSwiggyMcpToolClient.diagnosticText(callResult, objectMapper))
				.isEqualTo("⚠ Human-readable provider warning");
	}

	@Test
	void selectsJsonTextWhenItFollowsASeparateWarningBlock() {
		var callResult = McpSchema.CallToolResult.builder()
				.addTextContent("⚠ Human-readable provider warning")
				.addTextContent("{\"success\":true,\"data\":{\"restaurants\":[]}}")
				.isError(false)
				.build();

		var payload = SpringAiSwiggyMcpToolClient.payload(callResult, objectMapper);

		assertThat(payload).isEqualTo(
				"{\"success\":true,\"data\":{\"restaurants\":[]}}");
	}
}
