package com.learning.foodagent.model;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.learning.foodagent.agent.AgentModelAction;
import com.learning.foodagent.agent.AgentModelGateway;
import com.learning.foodagent.agent.AgentModelRequest;
import com.learning.foodagent.agent.ToolObservation;

@Component
@ConditionalOnProperty(name = "food-agent.model.provider", havingValue = "ollama", matchIfMissing = true)
final class OllamaAgentModelGateway implements AgentModelGateway {

	private final ChatClient chatClient;
	private final AgentSystemPrompt systemPrompt;
	private final AgentDecisionDecoder decisionDecoder;
	private final ModelCallExecutor callExecutor;

	OllamaAgentModelGateway(
			ChatClient modelChatClient,
			AgentSystemPrompt systemPrompt,
			AgentDecisionDecoder decisionDecoder,
			ModelCallExecutor callExecutor) {
		this.chatClient = modelChatClient;
		this.systemPrompt = systemPrompt;
		this.decisionDecoder = decisionDecoder;
		this.callExecutor = callExecutor;
	}

	@Override
	public AgentModelAction nextAction(AgentModelRequest request) {
		return callExecutor.execute(() -> invokeModel(request));
	}

	private AgentModelAction invokeModel(AgentModelRequest request) {
		try {
			String content = chatClient.prompt()
					.system(systemPrompt.text() + System.lineSeparator() + decisionDecoder.format())
					.user(userPrompt(request))
					.call()
					.content();
			return decisionDecoder.decode(content);
		}
		catch (ModelGatewayException exception) {
			throw exception;
		}
		catch (RuntimeException exception) {
			throw new ModelUnavailableException("Local model is unavailable", exception);
		}
	}

	private String userPrompt(AgentModelRequest request) {
		StringBuilder prompt = new StringBuilder()
				.append("<user_message>")
				.append(escape(request.userMessage()))
				.append("</user_message>\n")
				.append("<location_available>")
				.append(request.locationAvailable())
				.append("</location_available>\n")
				.append("<tool_observations>\n");

		for (ToolObservation observation : request.observations()) {
			prompt.append("<observation tool=\"")
					.append(escape(observation.toolName()))
					.append("\" status=\"")
					.append(observation.status())
					.append("\">")
					.append(escape(observation.content()))
					.append("</observation>\n");
		}
		return prompt.append("</tool_observations>").toString();
	}

	private String escape(String value) {
		return value.replace("&", "&amp;")
				.replace("<", "&lt;")
				.replace(">", "&gt;")
				.replace("\"", "&quot;")
				.replace("'", "&apos;");
	}
}
