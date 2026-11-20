package com.learning.foodagent.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;

import com.learning.foodagent.discovery.RestaurantDiscoveryGateway;
import com.learning.foodagent.domain.discovery.FoodProvider;
import com.learning.foodagent.domain.discovery.GeoCoordinates;
import com.learning.foodagent.domain.discovery.RestaurantSearchCriteria;
import com.learning.foodagent.domain.discovery.RestaurantSearchResult;
import com.learning.foodagent.domain.discovery.RestaurantSummary;

@SpringBootTest(properties = {
		"food-agent.model.provider=ollama",
		"food-agent.providers.swiggy.enabled=false",
		"spring.ai.mcp.client.enabled=false"
})
@Import(OllamaAgentLiveEvaluationTest.EvaluationProviderConfiguration.class)
@EnabledIfEnvironmentVariable(named = "OLLAMA_AGENT_LIVE_TEST", matches = "(?i)true")
class OllamaAgentLiveEvaluationTest {

	@Autowired
	private AgentOrchestrator orchestrator;

	@Test
	void selectsRestaurantSearchToolAndReturnsGroundedResults() {
		AgentTurnResult result = orchestrator.handle(new AgentTurnCommand(
				"Find vegetarian dosa restaurants nearby",
				new GeoCoordinates(12.9716, 77.5946)));

		assertEquals(AgentTurnResult.Outcome.COMPLETED, result.outcome());
		assertFalse(result.restaurantSearches().isEmpty());
		assertEquals("Learning Dosa House",
				result.restaurantSearches().getFirst().restaurants().getFirst().name());
		assertTrue(result.trace().stream().anyMatch(event -> event.type() == AgentTraceEvent.Type.TOOL_COMPLETED));
	}

	@Test
	void asksForLocationBeforeSearching() {
		AgentTurnResult result = orchestrator.handle(new AgentTurnCommand(
				"Find vegetarian dosa restaurants nearby", null));

		assertEquals(AgentTurnResult.Outcome.CLARIFICATION_REQUIRED, result.outcome());
		assertTrue(result.restaurantSearches().isEmpty());
		assertFalse(result.message().isBlank());
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class EvaluationProviderConfiguration {

		@Bean
		RestaurantDiscoveryGateway evaluationSwiggyGateway() {
			return new RestaurantDiscoveryGateway() {
				@Override
				public FoodProvider provider() {
					return FoodProvider.SWIGGY;
				}

				@Override
				public RestaurantSearchResult search(RestaurantSearchCriteria criteria) {
					return new RestaurantSearchResult(
							FoodProvider.SWIGGY,
							criteria.query(),
							List.of(new RestaurantSummary(
									"evaluation-restaurant",
									"Learning Dosa House",
									List.of("South Indian", "Vegetarian"),
									BigDecimal.valueOf(4.5),
									"1K+",
									"₹250 for two",
									"Central",
									BigDecimal.valueOf(2.0),
									25,
									true,
									null,
									null,
									"OPEN")),
							null,
							false,
							Instant.now());
				}
			};
		}
	}
}
