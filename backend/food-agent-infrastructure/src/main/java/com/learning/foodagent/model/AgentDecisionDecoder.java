package com.learning.foodagent.model;

import java.util.Map;

import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Component;

import com.learning.foodagent.agent.AgentModelAction;

@Component
final class AgentDecisionDecoder {

	private final BeanOutputConverter<OllamaAgentDecision> outputConverter =
			new BeanOutputConverter<>(OllamaAgentDecision.class);

	String format() {
		return outputConverter.getFormat();
	}

	AgentModelAction decode(String content) {
		final OllamaAgentDecision decision;
		try {
			decision = outputConverter.convert(content);
		}
		catch (RuntimeException exception) {
			throw new InvalidModelResponseException("Local model returned a malformed agent action", exception);
		}
		if (decision == null || decision.action() == null) {
			throw new InvalidModelResponseException("Local model returned an incomplete agent action");
		}

		try {
			return switch (decision.action()) {
				case TOOL_CALL -> toolCall(decision);
				case FINAL_ANSWER -> new AgentModelAction.FinalAnswer(decision.message());
				case CLARIFICATION -> new AgentModelAction.Clarification(decision.message());
				case UNSUPPORTED -> new AgentModelAction.Unsupported(decision.message());
			};
		}
		catch (IllegalArgumentException | NullPointerException exception) {
			throw new InvalidModelResponseException("Local model agent action failed schema validation", exception);
		}
	}

	private AgentModelAction toolCall(OllamaAgentDecision decision) {
		ToolArguments arguments = decision.arguments();
		if (arguments == null || arguments.query() == null || arguments.query().isBlank()
				|| arguments.query().length() > 200) {
			throw new IllegalArgumentException("Tool query is missing or invalid");
		}
		return new AgentModelAction.ToolCall(decision.toolName(), Map.of("query", arguments.query()));
	}

	private record OllamaAgentDecision(
			Action action,
			String toolName,
			ToolArguments arguments,
			String message) {

		private enum Action {
			TOOL_CALL,
			FINAL_ANSWER,
			CLARIFICATION,
			UNSUPPORTED
		}
	}

	private record ToolArguments(String query) {
	}
}
