package com.learning.foodagent.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.learning.foodagent.discovery.RestaurantDiscoveryGateway;
import com.learning.foodagent.discovery.RestaurantDiscoveryService;
import com.learning.foodagent.domain.discovery.FoodProvider;
import com.learning.foodagent.domain.discovery.GeoCoordinates;
import com.learning.foodagent.domain.discovery.RestaurantSearchCriteria;
import com.learning.foodagent.domain.discovery.RestaurantSearchResult;
import com.learning.foodagent.domain.discovery.RestaurantSummary;

class RestaurantSearchAgentToolTest {

	private static final GeoCoordinates LOCATION = new GeoCoordinates(12.9716, 77.5946);
	private static final AgentLimits LIMITS = new AgentLimits(3, 6_000, 2, Duration.ofSeconds(10));

	@Test
	void searchesSwiggyAndBoundsResults() {
		RestaurantSearchResult providerResult = new RestaurantSearchResult(
				FoodProvider.SWIGGY,
				"dosa",
				List.of(restaurant("First"), restaurant("Second"), restaurant("Third")),
				"next",
				true,
				Instant.EPOCH);
		var tool = new RestaurantSearchAgentTool(
				new RestaurantDiscoveryService(List.of(new FixedGateway(providerResult))), LIMITS);

		AgentToolResult result = tool.execute(Map.of("query", "dosa"), new AgentTurnContext(LOCATION));

		assertEquals(2, result.restaurantSearches().getFirst().restaurants().size());
		assertTrue(result.restaurantSearches().getFirst().hasMore());
		assertTrue(result.observation().contains("name=First"));
	}

	@Test
	void rejectsUnsupportedArguments() {
		var tool = toolWithEmptyResult();

		assertThrows(InvalidAgentActionException.class,
				() -> tool.execute(Map.of("query", "dosa", "url", "https://example.com"),
						new AgentTurnContext(LOCATION)));
	}

	@Test
	void rejectsSearchWithoutResolvedLocation() {
		var tool = toolWithEmptyResult();

		assertThrows(InvalidAgentActionException.class,
				() -> tool.execute(Map.of("query", "dosa"), new AgentTurnContext(null)));
	}

	private RestaurantSearchAgentTool toolWithEmptyResult() {
		return new RestaurantSearchAgentTool(
				new RestaurantDiscoveryService(List.of(new FixedGateway(new RestaurantSearchResult(
						FoodProvider.SWIGGY, "dosa", List.of(), null, false, Instant.EPOCH)))), LIMITS);
	}

	private RestaurantSummary restaurant(String name) {
		return new RestaurantSummary(
				name.toLowerCase(), name, List.of("South Indian"), BigDecimal.valueOf(4.2),
				"100+", "₹200 for two", "Central", BigDecimal.ONE, 20, true,
				null, null, "OPEN");
	}

	private record FixedGateway(RestaurantSearchResult result) implements RestaurantDiscoveryGateway {

		@Override
		public FoodProvider provider() {
			return FoodProvider.SWIGGY;
		}

		@Override
		public RestaurantSearchResult search(RestaurantSearchCriteria criteria) {
			return result;
		}
	}
}
