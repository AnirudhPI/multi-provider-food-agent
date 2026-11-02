package com.learning.foodagent.discovery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.learning.foodagent.domain.discovery.FoodProvider;
import com.learning.foodagent.domain.discovery.GeoCoordinates;
import com.learning.foodagent.domain.discovery.RestaurantSearchCriteria;
import com.learning.foodagent.domain.discovery.RestaurantSearchResult;

class RestaurantDiscoveryServiceTest {

	private static final RestaurantSearchCriteria CRITERIA = new RestaurantSearchCriteria(
			"dosa", new GeoCoordinates(12.9716, 77.5946), null);

	@Test
	void routesSearchToMatchingProviderGateway() {
		var expected = result(FoodProvider.SWIGGY);
		var service = new RestaurantDiscoveryService(List.of(new FixedGateway(FoodProvider.SWIGGY, expected)));

		assertEquals(expected, service.search(FoodProvider.SWIGGY, CRITERIA));
	}

	@Test
	void reportsProviderThatIsNotConfigured() {
		var service = new RestaurantDiscoveryService(List.of());

		assertThrows(ProviderUnavailableException.class,
				() -> service.search(FoodProvider.SWIGGY, CRITERIA));
	}

	@Test
	void rejectsDuplicateGatewayForSameProvider() {
		var first = new FixedGateway(FoodProvider.SWIGGY, result(FoodProvider.SWIGGY));
		var second = new FixedGateway(FoodProvider.SWIGGY, result(FoodProvider.SWIGGY));

		assertThrows(IllegalStateException.class,
				() -> new RestaurantDiscoveryService(List.of(first, second)));
	}

	private static RestaurantSearchResult result(FoodProvider provider) {
		return new RestaurantSearchResult(provider, "dosa", List.of(), null, false, Instant.EPOCH);
	}

	private record FixedGateway(FoodProvider provider, RestaurantSearchResult result)
			implements RestaurantDiscoveryGateway {

		@Override
		public RestaurantSearchResult search(RestaurantSearchCriteria criteria) {
			return result;
		}
	}
}
