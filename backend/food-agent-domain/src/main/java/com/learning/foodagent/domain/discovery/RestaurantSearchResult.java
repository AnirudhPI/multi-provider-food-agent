package com.learning.foodagent.domain.discovery;

import java.time.Instant;
import java.util.List;
import java.util.Objects;

public record RestaurantSearchResult(
		FoodProvider provider,
		String query,
		List<RestaurantSummary> restaurants,
		String nextContinuationToken,
		boolean hasMore,
		Instant observedAt) {

	public RestaurantSearchResult {
		provider = Objects.requireNonNull(provider, "provider must not be null");
		query = Objects.requireNonNull(query, "query must not be null");
		restaurants = List.copyOf(Objects.requireNonNull(restaurants, "restaurants must not be null"));
		observedAt = Objects.requireNonNull(observedAt, "observedAt must not be null");
	}
}
