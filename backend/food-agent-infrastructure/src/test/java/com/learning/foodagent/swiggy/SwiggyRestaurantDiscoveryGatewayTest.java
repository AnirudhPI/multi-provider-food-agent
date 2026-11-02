package com.learning.foodagent.swiggy;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import java.net.http.HttpTimeoutException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import org.junit.jupiter.api.Test;
import com.learning.foodagent.discovery.ProviderTimeoutException;
import com.learning.foodagent.discovery.ProviderAuthenticationException;
import com.learning.foodagent.domain.discovery.GeoCoordinates;
import com.learning.foodagent.domain.discovery.RestaurantSearchCriteria;

import io.modelcontextprotocol.spec.McpTransportException;
import tools.jackson.databind.json.JsonMapper;

class SwiggyRestaurantDiscoveryGatewayTest {

	private final FakeSwiggyMcpToolClient client = new FakeSwiggyMcpToolClient();
	private final SwiggyRestaurantDiscoveryGateway gateway = new SwiggyRestaurantDiscoveryGateway(
			client,
			new SwiggyRestaurantResponseDecoder(
					JsonMapper.builder().build(),
					Clock.fixed(Instant.parse("2026-11-02T12:00:00Z"), ZoneOffset.UTC)));

	@Test
	void mapsCanonicalCriteriaToAllowListedReadOnlyTool() {
		client.result = new SwiggyMcpToolClient.ToolResult(
				"{\"success\":true,\"data\":{\"restaurants\":[],\"query\":\"dosa\",\"hasMore\":false}}",
				null,
				false);

		var criteria = new RestaurantSearchCriteria(
				"dosa",
				new GeoCoordinates(12.9716, 77.5946),
				"10");
		var result = gateway.search(criteria);

		assertThat(client.toolName).isEqualTo("search_restaurants");
		assertThat(client.arguments).containsEntry("query", "dosa")
				.containsEntry("latitude", 12.9716)
				.containsEntry("longitude", 77.5946)
				.containsEntry("offset", 10L);
		assertThat(result.restaurants()).isEmpty();
	}

	@Test
	void classifiesTransportTimeout() {
		client.failure = new McpTransportException(new HttpTimeoutException("timed out"));

		assertThatThrownBy(() -> gateway.search(new RestaurantSearchCriteria(
				"dosa",
				new GeoCoordinates(12.9716, 77.5946),
				null)))
				.isInstanceOf(ProviderTimeoutException.class)
				.hasMessage("Swiggy restaurant search timed out");
	}

	@Test
	void classifiesMissingOrExpiredAuthentication() {
		client.failure = new SwiggyMcpToolClient.AuthenticationFailure(new IllegalStateException("401"));

		assertThatThrownBy(() -> gateway.search(new RestaurantSearchCriteria(
				"dosa",
				new GeoCoordinates(12.9716, 77.5946),
				null)))
				.isInstanceOf(ProviderAuthenticationException.class)
				.hasMessage("Swiggy authentication is missing or expired");
	}

	private static final class FakeSwiggyMcpToolClient implements SwiggyMcpToolClient {

		private String toolName;
		private Map<String, Object> arguments;
		private ToolResult result;
		private RuntimeException failure;

		@Override
		public ToolResult call(String toolName, Map<String, Object> arguments) {
			this.toolName = toolName;
			this.arguments = Map.copyOf(arguments);
			if (failure != null) {
				throw failure;
			}
			return result;
		}
	}
}
