package com.learning.foodagent.domain.discovery;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

class RestaurantSummaryTest {

	@Test
	void preservesMissingOptionalIdentifierAndCopiesCleanCuisineValues() {
		var restaurant = restaurant(null, new BigDecimal("4.5"), new BigDecimal("2.3"), 20,
				List.of(" South Indian ", "Beverages"));

		assertNull(restaurant.providerRestaurantId());
		assertEquals(List.of("South Indian", "Beverages"), restaurant.cuisines());
	}

	@Test
	void rejectsImpossibleProviderMeasurements() {
		assertThrows(IllegalArgumentException.class,
				() -> restaurant("id", new BigDecimal("5.1"), new BigDecimal("2.3"), 20, List.of()));
		assertThrows(IllegalArgumentException.class,
				() -> restaurant("id", new BigDecimal("4.5"), new BigDecimal("-1"), 20, List.of()));
		assertThrows(IllegalArgumentException.class,
				() -> restaurant("id", new BigDecimal("4.5"), new BigDecimal("2.3"), -1, List.of()));
	}

	private static RestaurantSummary restaurant(
			String id,
			BigDecimal rating,
			BigDecimal distance,
			Integer deliveryMinutes,
			List<String> cuisines) {
		return new RestaurantSummary(
				id,
				"South Kitchen",
				cuisines,
				rating,
				"1K+",
				"₹300 for two",
				"Learning Layout",
				distance,
				deliveryMinutes,
				true,
				null,
				null,
				"OPEN");
	}
}
