package com.learning.foodagent.model;

import java.time.Instant;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.learning.foodagent.agent.AgentModelAction;
import com.learning.foodagent.agent.AgentModelGateway;
import com.learning.foodagent.agent.AgentModelRequest;
import com.learning.foodagent.agent.RestaurantSearchAgentTool;
import com.learning.foodagent.agent.ToolObservation;

@Component
@ConditionalOnProperty(name = "food-agent.model.provider", havingValue = "fake")
final class FakeLocalModelGateway implements LocalModelGateway, AgentModelGateway {

	private final ModelProperties properties;

	FakeLocalModelGateway(ModelProperties properties) {
		this.properties = properties;
	}

	@Override
	public ModelHealth health() {
		return new ModelHealth(ModelHealth.Status.UP, "fake", properties.model(), "READY", Instant.now());
	}

	@Override
	public ModelResult generate(ModelPrompt prompt) {
		return new ModelResult(
				ModelResult.Category.FOOD_REQUEST,
				"Deterministic fake response for: " + prompt.prompt());
	}

	@Override
	public AgentModelAction nextAction(AgentModelRequest request) {
		if (!request.locationAvailable()) {
			return new AgentModelAction.Clarification("Please provide a location before I search for restaurants.");
		}
		if (request.observations().isEmpty()) {
			String query = request.userMessage().length() <= 200
					? request.userMessage()
					: request.userMessage().substring(0, 200);
			return new AgentModelAction.ToolCall(RestaurantSearchAgentTool.NAME, Map.of("query", query));
		}
		ToolObservation lastObservation = request.observations().getLast();
		if (lastObservation.status() == ToolObservation.Status.FAILURE) {
			return new AgentModelAction.FinalAnswer("I could not search Swiggy because the provider request failed.");
		}
		return new AgentModelAction.FinalAnswer("I found restaurant results from Swiggy.");
	}
}
