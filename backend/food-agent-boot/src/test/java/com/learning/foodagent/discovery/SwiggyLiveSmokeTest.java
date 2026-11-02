package com.learning.foodagent.discovery;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.learning.foodagent.domain.discovery.FoodProvider;
import com.learning.foodagent.domain.discovery.GeoCoordinates;
import com.learning.foodagent.domain.discovery.RestaurantSearchCriteria;

@SpringBootTest(properties = {
		"food-agent.model.provider=fake",
		"food-agent.providers.swiggy.enabled=true",
		"spring.ai.mcp.client.enabled=true"
})
@EnabledIfEnvironmentVariable(named = "SWIGGY_MCP_LIVE_TEST", matches = "true")
class SwiggyLiveSmokeTest {

	@Autowired
	private RestaurantDiscoveryService discoveryService;

	@Test
	void returnsNormalizedRestaurantResultsFromSwiggy() {
		double latitude = requiredDouble("SWIGGY_MCP_TEST_LATITUDE");
		double longitude = requiredDouble("SWIGGY_MCP_TEST_LONGITUDE");
		String query = System.getenv().getOrDefault("SWIGGY_MCP_TEST_QUERY", "dosa");

		var result = discoveryService.search(
				FoodProvider.SWIGGY,
				new RestaurantSearchCriteria(query, new GeoCoordinates(latitude, longitude), null));

		assertThat(result.provider()).isEqualTo(FoodProvider.SWIGGY);
		assertThat(result.restaurants()).isNotEmpty();
		assertThat(result.restaurants()).allSatisfy(restaurant -> {
			assertThat(restaurant.name()).isNotBlank();
		});
	}

	private static double requiredDouble(String variable) {
		String value = System.getenv(variable);
		if (value == null || value.isBlank()) {
			throw new IllegalStateException(variable + " is required for the live smoke test");
		}
		return Double.parseDouble(value);
	}
}
