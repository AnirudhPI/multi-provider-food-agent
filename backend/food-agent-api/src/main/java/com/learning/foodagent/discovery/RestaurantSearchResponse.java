package com.learning.foodagent.discovery;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import com.learning.foodagent.domain.discovery.RestaurantSearchResult;
import com.learning.foodagent.domain.discovery.RestaurantSummary;

record RestaurantSearchResponse(
		String provider,
		String query,
		List<RestaurantResponse> restaurants,
		String nextContinuationToken,
		boolean hasMore,
		Instant observedAt) {

	static RestaurantSearchResponse from(RestaurantSearchResult result) {
		return new RestaurantSearchResponse(
				result.provider().name(),
				result.query(),
				result.restaurants().stream().map(RestaurantResponse::from).toList(),
				result.nextContinuationToken(),
				result.hasMore(),
				result.observedAt());
	}

	record RestaurantResponse(
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

		static RestaurantResponse from(RestaurantSummary restaurant) {
			return new RestaurantResponse(
					restaurant.providerRestaurantId(),
					restaurant.name(),
					restaurant.cuisines(),
					restaurant.rating(),
					restaurant.totalRatingsDisplay(),
					restaurant.costForTwoDisplay(),
					restaurant.areaName(),
					restaurant.distanceKm(),
					restaurant.deliveryTimeMinutes(),
					restaurant.vegetarian(),
					restaurant.offer(),
					restaurant.imageUrl(),
					restaurant.availabilityStatus());
		}
	}
}
