package com.learning.foodagent.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

import com.learning.foodagent.agent.AgentModelAction;

class AgentDecisionDecoderTest {

	private final AgentDecisionDecoder decoder = new AgentDecisionDecoder();

	@Test
	void decodesToolCall() {
		AgentModelAction action = decoder.decode("""
				{"action":"TOOL_CALL","toolName":"search_restaurants","arguments":{"query":"dosa"},"message":null}
				""");

		AgentModelAction.ToolCall toolCall = assertInstanceOf(AgentModelAction.ToolCall.class, action);
		assertEquals("search_restaurants", toolCall.toolName());
		assertEquals("dosa", toolCall.arguments().get("query"));
	}

	@Test
	void decodesFinalAnswer() {
		AgentModelAction action = decoder.decode("""
				{"action":"FINAL_ANSWER","toolName":null,"arguments":null,"message":"Two results were found."}
				""");

		AgentModelAction.FinalAnswer answer = assertInstanceOf(AgentModelAction.FinalAnswer.class, action);
		assertEquals("Two results were found.", answer.message());
	}

	@Test
	void rejectsMalformedOutput() {
		assertThrows(InvalidModelResponseException.class, () -> decoder.decode("not-json"));
	}

	@Test
	void rejectsIncompleteToolCall() {
		assertThrows(InvalidModelResponseException.class, () -> decoder.decode("""
				{"action":"TOOL_CALL","toolName":null,"arguments":{},"message":null}
				"""));
	}

	@Test
	void rejectsBlankFinalMessage() {
		assertThrows(InvalidModelResponseException.class, () -> decoder.decode("""
				{"action":"FINAL_ANSWER","toolName":null,"arguments":null,"message":""}
				"""));
	}
}
