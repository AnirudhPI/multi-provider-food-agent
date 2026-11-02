package com.learning.foodagent.swiggy;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Component;

import com.learning.foodagent.discovery.ProviderResponseException;
import com.learning.foodagent.domain.discovery.FoodProvider;
import com.learning.foodagent.domain.discovery.RestaurantSearchResult;
import com.learning.foodagent.domain.discovery.RestaurantSummary;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;

@Component
final class SwiggyRestaurantResponseDecoder {

	private static final int MAX_PAYLOAD_CHARACTERS = 1_000_000;
	private static final int MAX_RESTAURANTS = 50;
	private static final int MAX_PROVIDER_ERROR_CODE_POINTS = 300;

	private final ObjectMapper objectMapper;
	private final Clock clock;

	SwiggyRestaurantResponseDecoder(ObjectMapper objectMapper, Clock providerClock) {
		this.objectMapper = objectMapper;
		this.clock = providerClock;
	}

	RestaurantSearchResult decode(String payload, String requestedQuery) {
		return decode(payload, requestedQuery, null);
	}

	RestaurantSearchResult decode(String payload, String requestedQuery, String providerMessage) {
		if (payload == null || payload.isBlank() || payload.length() > MAX_PAYLOAD_CHARACTERS) {
			throw invalidResponse();
		}

		try {
			JsonNode envelope = objectMapper.readTree(payload);
			JsonNode data;
			if (envelope.has("success")) {
				if (!envelope.path("success").asBoolean(false)) {
					throw providerRejection(envelope, providerMessage);
				}
				data = envelope.path("data");
			}
			else if (envelope.has("error")) {
				throw providerRejection(envelope, providerMessage);
			}
			else {
				data = envelope;
			}

			JsonNode restaurantNodes = data.path("restaurants");
			if (!data.isObject() || !restaurantNodes.isArray() || restaurantNodes.size() > MAX_RESTAURANTS) {
				throw invalidResponse();
			}

			List<RestaurantSummary> restaurants = new ArrayList<>(restaurantNodes.size());
			for (JsonNode restaurant : restaurantNodes) {
				restaurants.add(mapRestaurant(restaurant));
			}

			String query = text(data, "query");
			if (query == null) {
				query = requestedQuery;
			}
			String nextOffset = scalarText(data, "nextOffset");
			boolean hasMore = data.path("hasMore").asBoolean(nextOffset != null);

			return new RestaurantSearchResult(
					FoodProvider.SWIGGY,
					query,
					restaurants,
					nextOffset,
					hasMore,
					Instant.now(clock));
		}
		catch (ProviderResponseException exception) {
			throw exception;
		}
		catch (RuntimeException exception) {
			throw new ProviderResponseException(
					FoodProvider.SWIGGY,
					"Swiggy returned an invalid restaurant response",
					exception);
		}
	}

	private RestaurantSummary mapRestaurant(JsonNode restaurant) {
		String id = text(restaurant, "id");
		String name = text(restaurant, "name");
		if (name == null) {
			throw invalidResponse();
		}

		return new RestaurantSummary(
				id,
				name,
				strings(restaurant.path("cuisines")),
				decimal(restaurant, "avgRating"),
				scalarText(restaurant, "totalRatings"),
				text(restaurant, "costForTwo"),
				text(restaurant, "areaName"),
				decimal(restaurant, "distanceKm"),
				integer(restaurant, "deliveryTimeMinutes"),
				bool(restaurant, "veg"),
				text(restaurant, "offer"),
				text(restaurant, "imageUrl"),
				text(restaurant, "availabilityStatus"));
	}

	private static List<String> strings(JsonNode node) {
		if (!node.isArray()) {
			return List.of();
		}
		List<String> values = new ArrayList<>(node.size());
		for (JsonNode value : node) {
			if (value.isString() && !value.asString().isBlank()) {
				values.add(value.asString());
			}
		}
		return values;
	}

	private static String text(JsonNode node, String field) {
		JsonNode value = node.path(field);
		if (!value.isString()) {
			return null;
		}
		String text = value.asString().strip();
		return text.isEmpty() ? null : text;
	}

	private static String scalarText(JsonNode node, String field) {
		JsonNode value = node.path(field);
		if (!value.isString() && !value.isNumber()) {
			return null;
		}
		String text = value.asString().strip();
		return text.isEmpty() ? null : text;
	}

	private static BigDecimal decimal(JsonNode node, String field) {
		String value = scalarText(node, field);
		return value == null ? null : new BigDecimal(value);
	}

	private static Integer integer(JsonNode node, String field) {
		String value = scalarText(node, field);
		return value == null ? null : Integer.valueOf(value);
	}

	private static Boolean bool(JsonNode node, String field) {
		JsonNode value = node.path(field);
		return value.isBoolean() ? value.asBoolean() : null;
	}

	private static ProviderResponseException invalidResponse() {
		return new ProviderResponseException(FoodProvider.SWIGGY, "Swiggy returned an invalid restaurant response");
	}

	private static ProviderResponseException providerRejection(JsonNode envelope, String fallbackProviderMessage) {
		String providerMessage = text(envelope.path("error"), "message");
		if (providerMessage == null) {
			providerMessage = fallbackProviderMessage;
		}
		if (providerMessage == null) {
			return new ProviderResponseException(FoodProvider.SWIGGY, "Swiggy rejected the restaurant search");
		}

		String sanitizedMessage = providerMessage.replaceAll("\\p{Cntrl}+", " ")
				.replaceAll("\\s+", " ")
				.strip();
		if (sanitizedMessage.isEmpty()) {
			return new ProviderResponseException(FoodProvider.SWIGGY, "Swiggy rejected the restaurant search");
		}
		String boundedMessage = sanitizedMessage.codePoints()
				.limit(MAX_PROVIDER_ERROR_CODE_POINTS)
				.collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
				.toString();
		return new ProviderResponseException(
				FoodProvider.SWIGGY,
				"Swiggy rejected the restaurant search: " + boundedMessage);
	}
}
