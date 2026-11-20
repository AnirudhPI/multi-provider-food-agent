package com.learning.foodagent.agent;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;

import com.learning.foodagent.discovery.ProviderAuthenticationException;
import com.learning.foodagent.discovery.ProviderException;
import com.learning.foodagent.discovery.ProviderResponseException;
import com.learning.foodagent.discovery.ProviderTimeoutException;
import com.learning.foodagent.discovery.ProviderUnavailableException;
import com.learning.foodagent.domain.discovery.RestaurantSearchResult;

@Service
public final class AgentOrchestrator {

	private final AgentModelGateway modelGateway;
	private final Map<String, ReadOnlyAgentTool> tools;
	private final AgentLimits limits;
	private final AgentCallExecutor callExecutor;

	public AgentOrchestrator(
			AgentModelGateway modelGateway,
			List<ReadOnlyAgentTool> tools,
			AgentLimits limits,
			AgentCallExecutor callExecutor) {
		this.modelGateway = modelGateway;
		this.tools = toolRegistry(tools);
		this.limits = limits;
		this.callExecutor = callExecutor;
	}

	public AgentTurnResult handle(AgentTurnCommand command) {
		long deadlineNanos = System.nanoTime() + limits.timeout().toNanos();
		UUID turnId = UUID.randomUUID();
		List<AgentTraceEvent> trace = new ArrayList<>();
		List<ToolObservation> observations = new ArrayList<>();
		List<RestaurantSearchResult> searches = new ArrayList<>();
		trace.add(event(turnId, AgentTraceEvent.Type.TURN_STARTED, 0, null));

		for (int step = 1; step <= limits.maximumSteps(); step++) {
			ensureActive(deadlineNanos);
			trace.add(event(turnId, AgentTraceEvent.Type.MODEL_REQUESTED, step, null));
			AgentModelRequest modelRequest = new AgentModelRequest(
					command.message(), command.location() != null, observations);
			AgentModelAction action = callExecutor.execute(
					() -> modelGateway.nextAction(modelRequest), remaining(deadlineNanos));

			if (action instanceof AgentModelAction.ToolCall toolCall) {
				executeTool(turnId, toolCall, command, step, deadlineNanos, observations, searches, trace);
				continue;
			}
			if (action instanceof AgentModelAction.FinalAnswer answer) {
				if (observations.isEmpty()) {
					throw new InvalidAgentActionException("Local model returned an ungrounded final answer");
				}
				trace.add(event(turnId, AgentTraceEvent.Type.TURN_COMPLETED, step, null));
				return result(turnId, AgentTurnResult.Outcome.COMPLETED, answer.message(), searches, trace);
			}
			if (action instanceof AgentModelAction.Clarification clarification) {
				trace.add(event(turnId, AgentTraceEvent.Type.CLARIFICATION_REQUIRED, step, null));
				return result(turnId, AgentTurnResult.Outcome.CLARIFICATION_REQUIRED,
						clarification.message(), searches, trace);
			}
			if (action instanceof AgentModelAction.Unsupported unsupported) {
				trace.add(event(turnId, AgentTraceEvent.Type.UNSUPPORTED, step, null));
				return result(turnId, AgentTurnResult.Outcome.UNSUPPORTED, unsupported.message(), searches, trace);
			}
			throw new InvalidAgentActionException("Local model returned an unsupported action type");
		}

		throw new AgentStepLimitException("Agent turn exceeded the configured step limit");
	}

	private void executeTool(
			UUID turnId,
			AgentModelAction.ToolCall toolCall,
			AgentTurnCommand command,
			int step,
			long deadlineNanos,
			List<ToolObservation> observations,
			List<RestaurantSearchResult> searches,
			List<AgentTraceEvent> trace) {
		ReadOnlyAgentTool tool = tools.get(toolCall.toolName());
		if (tool == null) {
			throw new InvalidAgentActionException("Local model requested a tool that is not allowed");
		}

		trace.add(event(turnId, AgentTraceEvent.Type.TOOL_STARTED, step, tool.name()));
		try {
			AgentToolResult toolResult = callExecutor.execute(
					() -> tool.execute(toolCall.arguments(), new AgentTurnContext(command.location())),
					remaining(deadlineNanos));
			String boundedObservation = bound(toolResult.observation());
			observations.add(new ToolObservation(tool.name(), ToolObservation.Status.SUCCESS, boundedObservation));
			searches.addAll(toolResult.restaurantSearches());
			trace.add(event(turnId, AgentTraceEvent.Type.TOOL_COMPLETED, step, tool.name()));
		}
		catch (ProviderException exception) {
			observations.add(new ToolObservation(
					tool.name(), ToolObservation.Status.FAILURE, providerFailure(exception)));
			trace.add(event(turnId, AgentTraceEvent.Type.TOOL_FAILED, step, tool.name()));
		}
	}

	private Map<String, ReadOnlyAgentTool> toolRegistry(List<ReadOnlyAgentTool> configuredTools) {
		Map<String, ReadOnlyAgentTool> registry = new HashMap<>();
		for (ReadOnlyAgentTool tool : configuredTools) {
			ReadOnlyAgentTool previous = registry.put(tool.name(), tool);
			if (previous != null) {
				throw new IllegalStateException("Multiple agent tools configured with name " + tool.name());
			}
		}
		return Map.copyOf(registry);
	}

	private String providerFailure(ProviderException exception) {
		String category = switch (exception) {
			case ProviderAuthenticationException ignored -> "AUTHENTICATION";
			case ProviderTimeoutException ignored -> "TIMEOUT";
			case ProviderUnavailableException ignored -> "UNAVAILABLE";
			case ProviderResponseException ignored -> "INVALID_RESPONSE";
			default -> "UNKNOWN";
		};
		return "provider=" + exception.provider() + ", failure=" + category;
	}

	private String bound(String observation) {
		if (observation.length() <= limits.maximumObservationCharacters()) {
			return observation;
		}
		String marker = "\n[TRUNCATED]";
		int contentLength = limits.maximumObservationCharacters() - marker.length();
		return observation.substring(0, contentLength) + marker;
	}

	private void ensureActive(long deadlineNanos) {
		if (Thread.currentThread().isInterrupted()) {
			throw new AgentTimeoutException("Agent turn was cancelled");
		}
		if (System.nanoTime() >= deadlineNanos) {
			throw new AgentTimeoutException("Agent turn exceeded its time limit");
		}
	}

	private Duration remaining(long deadlineNanos) {
		long remainingNanos = deadlineNanos - System.nanoTime();
		return remainingNanos <= 0 ? Duration.ZERO : Duration.ofNanos(remainingNanos);
	}

	private AgentTraceEvent event(UUID turnId, AgentTraceEvent.Type type, int step, String toolName) {
		return new AgentTraceEvent(turnId, type, step, toolName, Instant.now());
	}

	private AgentTurnResult result(
			UUID turnId,
			AgentTurnResult.Outcome outcome,
			String message,
			List<RestaurantSearchResult> searches,
			List<AgentTraceEvent> trace) {
		return new AgentTurnResult(turnId, outcome, message, searches, trace);
	}
}
