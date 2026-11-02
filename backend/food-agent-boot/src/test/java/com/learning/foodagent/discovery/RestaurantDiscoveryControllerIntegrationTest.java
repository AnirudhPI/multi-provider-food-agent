package com.learning.foodagent.discovery;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
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
import org.springframework.test.web.servlet.MockMvc;

import com.learning.foodagent.domain.discovery.FoodProvider;
import com.learning.foodagent.domain.discovery.RestaurantSearchCriteria;
import com.learning.foodagent.domain.discovery.RestaurantSearchResult;
import com.learning.foodagent.domain.discovery.RestaurantSummary;

@SpringBootTest(properties = "food-agent.model.provider=fake")
@AutoConfigureMockMvc
@Import(RestaurantDiscoveryControllerIntegrationTest.FakeProviderConfiguration.class)
class RestaurantDiscoveryControllerIntegrationTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void returnsCanonicalSwiggyRestaurantResults() throws Exception {
		mockMvc.perform(get("/api/v1/providers/swiggy/restaurants")
					.param("query", "dosa")
					.param("latitude", "12.9716")
					.param("longitude", "77.5946"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.provider").value("SWIGGY"))
				.andExpect(jsonPath("$.query").value("dosa"))
				.andExpect(jsonPath("$.restaurants[0].providerRestaurantId").value("restaurant-101"))
				.andExpect(jsonPath("$.restaurants[0].name").value("South Kitchen"))
				.andExpect(jsonPath("$.restaurants[0].rating").value(4.4));
	}

	@Test
	void rejectsCoordinatesOutsideDomainBounds() throws Exception {
		mockMvc.perform(get("/api/v1/providers/swiggy/restaurants")
					.param("query", "dosa")
					.param("latitude", "91")
					.param("longitude", "77.5946"))
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
					var restaurant = new RestaurantSummary(
							"restaurant-101",
							"South Kitchen",
							List.of("South Indian"),
							new BigDecimal("4.4"),
							"1.2K+",
							"₹300 for two",
							"Learning Layout",
							new BigDecimal("2.3"),
							28,
							true,
							"20% off",
							null,
							"OPEN");
					return new RestaurantSearchResult(
							FoodProvider.SWIGGY,
							criteria.query(),
							List.of(restaurant),
							null,
							false,
							Instant.parse("2026-11-02T12:00:00Z"));
				}
			};
		}
	}
}
