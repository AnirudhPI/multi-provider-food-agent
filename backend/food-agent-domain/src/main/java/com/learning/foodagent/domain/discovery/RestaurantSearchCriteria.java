package com.learning.foodagent.domain.discovery;

import java.util.Objects;

public record RestaurantSearchCriteria(String query, GeoCoordinates location, String continuationToken) {

	public RestaurantSearchCriteria {
		query = Objects.requireNonNull(query, "query must not be null").strip();
		if (query.isEmpty()) {
			throw new IllegalArgumentException("query must not be blank");
		}
		if (query.length() > 200) {
			throw new IllegalArgumentException("query must not exceed 200 characters");
		}
		location = Objects.requireNonNull(location, "location must not be null");
		continuationToken = normalize(continuationToken);
		if (continuationToken != null && continuationToken.length() > 200) {
			throw new IllegalArgumentException("continuationToken must not exceed 200 characters");
		}
	}

	private static String normalize(String value) {
		if (value == null) {
			return null;
		}
		String normalized = value.strip();
		return normalized.isEmpty() ? null : normalized;
	}
}
