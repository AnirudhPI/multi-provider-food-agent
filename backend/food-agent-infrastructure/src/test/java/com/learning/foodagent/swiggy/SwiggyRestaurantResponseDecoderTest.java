package com.learning.foodagent.swiggy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import com.learning.foodagent.discovery.ProviderResponseException;
import com.learning.foodagent.domain.discovery.FoodProvider;

import tools.jackson.databind.json.JsonMapper;

class SwiggyRestaurantResponseDecoderTest {

	private static final Instant OBSERVED_AT = Instant.parse("2026-11-02T12:00:00Z");

	private final SwiggyRestaurantResponseDecoder decoder = new SwiggyRestaurantResponseDecoder(
			JsonMapper.builder().build(),
			Clock.fixed(OBSERVED_AT, ZoneOffset.UTC));

	@Test
	void normalizesDocumentedRestaurantSearchEnvelope() throws IOException {
		var result = decoder.decode(fixture("search-restaurants-success.json"), "dosa");

		assertThat(result.provider()).isEqualTo(FoodProvider.SWIGGY);
		assertThat(result.query()).isEqualTo("dosa");
		assertThat(result.nextContinuationToken()).isEqualTo("10");
		assertThat(result.hasMore()).isTrue();
		assertThat(result.observedAt()).isEqualTo(OBSERVED_AT);
		assertThat(result.restaurants()).singleElement().satisfies(restaurant -> {
			assertThat(restaurant.providerRestaurantId()).isEqualTo("restaurant-101");
			assertThat(restaurant.name()).isEqualTo("South Kitchen");
			assertThat(restaurant.cuisines()).containsExactly("South Indian", "Beverages");
			assertThat(restaurant.rating()).isEqualByComparingTo(new BigDecimal("4.4"));
			assertThat(restaurant.totalRatingsDisplay()).isEqualTo("1.2K+");
			assertThat(restaurant.costForTwoDisplay()).isEqualTo("₹300 for two");
			assertThat(restaurant.deliveryTimeMinutes()).isEqualTo(28);
		});
	}

	@Test
	void normalizesMcpStructuredContentWithoutTheOuterToolEnvelope() {
		var result = decoder.decode(
				"{\"restaurants\":[{\"id\":\"204661\",\"name\":\"Sri Udupi Park\"}],\"query\":\"dosa\",\"hasMore\":false}",
				"dosa");

		assertThat(result.query()).isEqualTo("dosa");
		assertThat(result.hasMore()).isFalse();
		assertThat(result.restaurants()).singleElement().satisfies(restaurant -> {
			assertThat(restaurant.providerRestaurantId()).isEqualTo("204661");
			assertThat(restaurant.name()).isEqualTo("Sri Udupi Park");
		});
	}

	@Test
	void surfacesOnlyTheBoundedSanitizedProviderFailureMessage() {
		assertThatThrownBy(() -> decoder.decode(
				"{\"success\":false,\"error\":{\"message\":\"Invalid location\\nretry with coordinates\"},\"privateDetail\":\"must not leak\"}",
				"dosa"))
				.isInstanceOf(ProviderResponseException.class)
				.hasMessage("Swiggy rejected the restaurant search: Invalid location retry with coordinates")
				.hasMessageNotContaining("privateDetail")
				.hasMessageNotContaining("must not leak");
	}

	@Test
	void fallsBackToSeparateMcpDiagnosticWhenFailureEnvelopeOmitsMessage() {
		assertThatThrownBy(() -> decoder.decode(
				"{\"success\":false,\"error\":{}}",
				"dosa",
				"⚠ Search unavailable for this location"))
				.isInstanceOf(ProviderResponseException.class)
				.hasMessage("Swiggy rejected the restaurant search: ⚠ Search unavailable for this location");
	}

	@Test
	void preservesUnknownIdentifierInsteadOfInventingOne() {
		var result = decoder.decode(
				"{\"success\":true,\"data\":{\"restaurants\":[{\"name\":\"Missing id\"}]}}",
				"dosa");

		assertThat(result.restaurants()).singleElement()
				.extracting(restaurant -> restaurant.providerRestaurantId())
				.isNull();
	}

	private String fixture(String name) throws IOException {
		try (var input = getClass().getResourceAsStream("/swiggy/" + name)) {
			if (input == null) {
				throw new IOException("Missing fixture " + name);
			}
			return new String(input.readAllBytes(), StandardCharsets.UTF_8);
		}
	}
}
