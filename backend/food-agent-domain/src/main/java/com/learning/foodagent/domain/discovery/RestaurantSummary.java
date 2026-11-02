package com.learning.foodagent.domain.discovery;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

public record RestaurantSummary(
		String providerRestaurantId,
		String name,
		List<String> cuisines,
		BigDecimal rating,
		String totalRatingsDisplay,
		String costForTwoDisplay,
		String areaName,
		BigDecimal distanceKm,
		Integer deliveryTimeMinutes,
		Boolean vegetarian,
		String offer,
		String imageUrl,
		String availabilityStatus) {

	public RestaurantSummary {
		providerRestaurantId = optional(providerRestaurantId);
		name = required(name, "name");
		cuisines = cuisines == null ? List.of() : cuisines.stream()
				.filter(Objects::nonNull)
				.map(String::strip)
				.filter(value -> !value.isEmpty())
				.toList();
		if (rating != null && (rating.signum() < 0 || rating.compareTo(BigDecimal.valueOf(5)) > 0)) {
			throw new IllegalArgumentException("rating must be between 0 and 5");
		}
		if (distanceKm != null && distanceKm.signum() < 0) {
			throw new IllegalArgumentException("distanceKm must not be negative");
		}
		if (deliveryTimeMinutes != null && deliveryTimeMinutes < 0) {
			throw new IllegalArgumentException("deliveryTimeMinutes must not be negative");
		}
		totalRatingsDisplay = optional(totalRatingsDisplay);
		costForTwoDisplay = optional(costForTwoDisplay);
		areaName = optional(areaName);
		offer = optional(offer);
		imageUrl = optional(imageUrl);
		availabilityStatus = optional(availabilityStatus);
	}

	private static String optional(String value) {
		if (value == null) {
			return null;
		}
		String normalized = value.strip();
		return normalized.isEmpty() ? null : normalized;
	}

	private static String required(String value, String field) {
		String normalized = Objects.requireNonNull(value, field + " must not be null").strip();
		if (normalized.isEmpty()) {
			throw new IllegalArgumentException(field + " must not be blank");
		}
		return normalized;
	}
}
