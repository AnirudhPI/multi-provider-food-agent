package com.learning.foodagent.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;

import com.learning.foodagent.discovery.ProviderUnavailableException;
import com.learning.foodagent.domain.discovery.FoodProvider;
import com.learning.foodagent.domain.discovery.GeoCoordinates;
import com.learning.foodagent.domain.discovery.RestaurantSearchResult;

class AgentOrchestratorTest {

	private static final GeoCoordinates LOCATION = new GeoCoordinates(12.9716, 77.5946);
	private static final AgentLimits LIMITS = new AgentLimits(3, 200, 5, Duration.ofSeconds(2));

	@Test
	void executesAllowedToolAndReturnsGroundedResult() {
		var model = new ScriptedModelGateway(
				new AgentModelAction.ToolCall("search_restaurants", Map.of("query", "dosa")),
				new AgentModelAction.FinalAnswer("I found grounded dosa results."));
		RestaurantSearchResult searchResult = searchResult();
		ReadOnlyAgentTool tool = fixedTool("search_restaurants", new AgentToolResult("one dosa restaurant", List.of(searchResult)));

		AgentTurnResult result = orchestrator(model, List.of(tool), LIMITS)
				.handle(new AgentTurnCommand("Find dosa", LOCATION));

		assertEquals(AgentTurnResult.Outcome.COMPLETED, result.outcome());
		assertEquals(List.of(searchResult), result.restaurantSearches());
		assertEquals(2, model.requests.size());
		assertEquals(ToolObservation.Status.SUCCESS, model.requests.get(1).observations().getFirst().status());
		assertTrue(result.trace().stream().anyMatch(event -> event.type() == AgentTraceEvent.Type.TOOL_COMPLETED));
	}

	@Test
	void returnsClarificationWithoutExecutingTool() {
		var model = new ScriptedModelGateway(
				new AgentModelAction.Clarification("Which location should I search near?"));

		AgentTurnResult result = orchestrator(model, List.of(), LIMITS)
				.handle(new AgentTurnCommand("Find dosa", null));

		assertEquals(AgentTurnResult.Outcome.CLARIFICATION_REQUIRED, result.outcome());
		assertTrue(result.restaurantSearches().isEmpty());
	}

	@Test
	void rejectsToolThatIsNotAllowListed() {
		var model = new ScriptedModelGateway(
				new AgentModelAction.ToolCall("run_shell", Map.of("command", "anything")));

		assertThrows(InvalidAgentActionException.class,
				() -> orchestrator(model, List.of(), LIMITS)
						.handle(new AgentTurnCommand("Do something", LOCATION)));
	}

	@Test
	void rejectsFinalAnswerWithoutAnyToolObservation() {
		var model = new ScriptedModelGateway(new AgentModelAction.FinalAnswer("An invented restaurant exists."));

		assertThrows(InvalidAgentActionException.class,
				() -> orchestrator(model, List.of(), LIMITS)
						.handle(new AgentTurnCommand("Find dosa", LOCATION)));
	}

	@Test
	void returnsCategorizedProviderFailureToModel() {
		var model = new ScriptedModelGateway(
				new AgentModelAction.ToolCall("search_restaurants", Map.of("query", "dosa")),
				new AgentModelAction.FinalAnswer("Swiggy is currently unavailable."));
		ReadOnlyAgentTool failingTool = new ReadOnlyAgentTool() {
			@Override
			public String name() {
				return "search_restaurants";
			}

			@Override
			public AgentToolResult execute(Map<String, String> arguments, AgentTurnContext context) {
				throw new ProviderUnavailableException(FoodProvider.SWIGGY, "Sensitive provider detail");
			}
		};

		AgentTurnResult result = orchestrator(model, List.of(failingTool), LIMITS)
				.handle(new AgentTurnCommand("Find dosa", LOCATION));

		ToolObservation failure = model.requests.get(1).observations().getFirst();
		assertEquals(ToolObservation.Status.FAILURE, failure.status());
		assertEquals("provider=SWIGGY, failure=UNAVAILABLE", failure.content());
		assertTrue(result.restaurantSearches().isEmpty());
	}

	@Test
	void boundsObservationBeforeReturningItToModel() {
		AgentLimits smallObservationLimit = new AgentLimits(3, 100, 5, Duration.ofSeconds(2));
		var model = new ScriptedModelGateway(
				new AgentModelAction.ToolCall("search_restaurants", Map.of("query", "dosa")),
				new AgentModelAction.FinalAnswer("Done"));
		ReadOnlyAgentTool tool = fixedTool(
				"search_restaurants", new AgentToolResult("x".repeat(500), List.of()));

		orchestrator(model, List.of(tool), smallObservationLimit)
				.handle(new AgentTurnCommand("Find dosa", LOCATION));

		String content = model.requests.get(1).observations().getFirst().content();
		assertEquals(100, content.length());
		assertTrue(content.endsWith("[TRUNCATED]"));
	}

	@Test
	void stopsWhenModelExceedsStepLimit() {
		var model = new RepeatingModelGateway(
				new AgentModelAction.ToolCall("search_restaurants", Map.of("query", "dosa")));
		ReadOnlyAgentTool tool = fixedTool(
				"search_restaurants", new AgentToolResult("no results", List.of()));
		AgentLimits twoSteps = new AgentLimits(2, 200, 5, Duration.ofSeconds(2));

		assertThrows(AgentStepLimitException.class,
				() -> orchestrator(model, List.of(tool), twoSteps)
						.handle(new AgentTurnCommand("Find dosa", LOCATION)));
	}

	@Test
	void rejectsDuplicateToolNames() {
		ReadOnlyAgentTool first = fixedTool("same", new AgentToolResult("first", List.of()));
		ReadOnlyAgentTool second = fixedTool("same", new AgentToolResult("second", List.of()));

		assertThrows(IllegalStateException.class,
				() -> orchestrator(new RepeatingModelGateway(new AgentModelAction.Unsupported("No")),
						List.of(first, second), LIMITS));
	}

	private AgentOrchestrator orchestrator(
			AgentModelGateway model,
			List<ReadOnlyAgentTool> tools,
			AgentLimits limits) {
		return new AgentOrchestrator(
				model, tools, limits, new AgentCallExecutor(Executors.newVirtualThreadPerTaskExecutor()));
	}

	private ReadOnlyAgentTool fixedTool(String name, AgentToolResult result) {
		return new ReadOnlyAgentTool() {
			@Override
			public String name() {
				return name;
			}

			@Override
			public AgentToolResult execute(Map<String, String> arguments, AgentTurnContext context) {
				return result;
			}
		};
	}

	private RestaurantSearchResult searchResult() {
		return new RestaurantSearchResult(
				FoodProvider.SWIGGY, "dosa", List.of(), null, false, Instant.EPOCH);
	}

	private static final class ScriptedModelGateway implements AgentModelGateway {

		private final Deque<AgentModelAction> actions;
		private final List<AgentModelRequest> requests = new ArrayList<>();

		private ScriptedModelGateway(AgentModelAction... actions) {
			this.actions = new ArrayDeque<>(List.of(actions));
		}

		@Override
		public AgentModelAction nextAction(AgentModelRequest request) {
			requests.add(request);
			return actions.removeFirst();
		}
	}

	private record RepeatingModelGateway(AgentModelAction action) implements AgentModelGateway {

		@Override
		public AgentModelAction nextAction(AgentModelRequest request) {
			return action;
		}
	}
}
