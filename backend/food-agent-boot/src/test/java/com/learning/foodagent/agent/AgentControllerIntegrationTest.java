package com.learning.foodagent.agent;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import com.learning.foodagent.discovery.RestaurantDiscoveryGateway;
import com.learning.foodagent.domain.discovery.FoodProvider;
import com.learning.foodagent.domain.discovery.RestaurantSearchCriteria;
import com.learning.foodagent.domain.discovery.RestaurantSearchResult;
import com.learning.foodagent.domain.discovery.RestaurantSummary;

@SpringBootTest(properties = "food-agent.model.provider=fake")
@AutoConfigureMockMvc
@Import(AgentControllerIntegrationTest.FakeProviderConfiguration.class)
class AgentControllerIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void completesNaturalLanguageRestaurantSearch() throws Exception {
		mockMvc.perform(post("/api/v1/agent/turns")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "message": "Find vegetarian dosa",
							  "location": {"latitude": 12.9716, "longitude": 77.5946}
							}
							"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.outcome").value("COMPLETED"))
				.andExpect(jsonPath("$.message").value("I found restaurant results from Swiggy."))
				.andExpect(jsonPath("$.restaurantSearches[0].provider").value("SWIGGY"))
				.andExpect(jsonPath("$.restaurantSearches[0].restaurants[0].name").value("Learning Dosa House"))
				.andExpect(jsonPath("$.trace[0].type").value("TURN_STARTED"))
				.andExpect(jsonPath("$.trace[?(@.type == 'TOOL_COMPLETED')]").exists());
	}

	@Test
	void asksForLocationWhenItIsMissing() throws Exception {
		mockMvc.perform(post("/api/v1/agent/turns")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{"message":"Find dosa"}
							"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.outcome").value("CLARIFICATION_REQUIRED"))
				.andExpect(jsonPath("$.restaurantSearches").isEmpty());
	}

	@Test
	void rejectsInvalidCoordinatesBeforeCallingAgent() throws Exception {
		mockMvc.perform(post("/api/v1/agent/turns")
					.contentType(MediaType.APPLICATION_JSON)
					.content("""
							{
							  "message": "Find dosa",
							  "location": {"latitude": 100, "longitude": 77.5946}
							}
							"""))
				.andExpect(status().isBadRequest())
				.andExpect(jsonPath("$.code").value("INVALID_REQUEST"));
	}

	@TestConfiguration(proxyBeanMethods = false)
	static class FakeProviderConfiguration {

		@Bean
		RestaurantDiscoveryGateway fakeSwiggyRestaurantGateway() {
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
									"learning-dosa",
									"Learning Dosa House",
									List.of("South Indian"),
									BigDecimal.valueOf(4.4),
									"1K+",
									"₹250 for two",
									"Central",
									BigDecimal.valueOf(2.1),
									25,
									true,
									null,
									null,
									"OPEN")),
							null,
							false,
							Instant.EPOCH);
				}
			};
		}
	}
}
