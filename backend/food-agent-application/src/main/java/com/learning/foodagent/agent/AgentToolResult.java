package com.learning.foodagent.agent;

import java.util.List;
import java.util.Objects;

import com.learning.foodagent.domain.discovery.RestaurantSearchResult;

public record AgentToolResult(String observation, List<RestaurantSearchResult> restaurantSearches) {

	public AgentToolResult {
		observation = Objects.requireNonNull(observation, "observation must not be null").strip();
		restaurantSearches = List.copyOf(Objects.requireNonNull(
				restaurantSearches, "restaurantSearches must not be null"));
		if (observation.isEmpty()) {
			throw new IllegalArgumentException("observation must not be blank");
		}
	}
}
