package com.learning.foodagent.swiggy;

import java.net.http.HttpTimeoutException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeoutException;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.learning.foodagent.discovery.ProviderAuthenticationException;
import com.learning.foodagent.discovery.ProviderResponseException;
import com.learning.foodagent.discovery.ProviderTimeoutException;
import com.learning.foodagent.discovery.ProviderUnavailableException;
import com.learning.foodagent.discovery.RestaurantDiscoveryGateway;
import com.learning.foodagent.domain.discovery.FoodProvider;
import com.learning.foodagent.domain.discovery.RestaurantSearchCriteria;
import com.learning.foodagent.domain.discovery.RestaurantSearchResult;


@Component
@ConditionalOnProperty(prefix = "food-agent.providers.swiggy", name = "enabled", havingValue = "true")
final class SwiggyRestaurantDiscoveryGateway implements RestaurantDiscoveryGateway {

	private static final String TOOL_NAME = "search_restaurants";

	private final SwiggyMcpToolClient client;
	private final SwiggyRestaurantResponseDecoder decoder;

	SwiggyRestaurantDiscoveryGateway(SwiggyMcpToolClient client, SwiggyRestaurantResponseDecoder decoder) {
		this.client = client;
		this.decoder = decoder;
	}

	@Override
	public FoodProvider provider() {
		return FoodProvider.SWIGGY;
	}

	@Override
	public RestaurantSearchResult search(RestaurantSearchCriteria criteria) {
		Map<String, Object> arguments = new LinkedHashMap<>();
		arguments.put("query", criteria.query());
		arguments.put("latitude", criteria.location().latitude());
		arguments.put("longitude", criteria.location().longitude());
		if (criteria.continuationToken() != null) {
			try {
				arguments.put("offset", Long.valueOf(criteria.continuationToken()));
			}
			catch (NumberFormatException exception) {
				throw new IllegalArgumentException("Swiggy continuation token must be a non-negative number", exception);
			}
		}

		try {
			SwiggyMcpToolClient.ToolResult result = client.call(TOOL_NAME, arguments);
			if (result.error()) {
				throw new ProviderResponseException(FoodProvider.SWIGGY, "Swiggy rejected the restaurant search");
			}
			return decoder.decode(result.textPayload(), criteria.query(), result.providerMessage());
		}
		catch (ProviderResponseException exception) {
			throw exception;
		}
		catch (SwiggyMcpToolClient.AuthenticationFailure exception) {
			throw new ProviderAuthenticationException(
					FoodProvider.SWIGGY,
					"Swiggy authentication is missing or expired",
					exception);
		}
		catch (RuntimeException exception) {
			if (causedByTimeout(exception)) {
				throw new ProviderTimeoutException(FoodProvider.SWIGGY, "Swiggy restaurant search timed out", exception);
			}
			throw new ProviderUnavailableException(FoodProvider.SWIGGY, "Swiggy restaurant search is unavailable", exception);
		}
	}

	private static boolean causedByTimeout(Throwable exception) {
		Throwable current = exception;
		while (current != null) {
			if (current instanceof HttpTimeoutException || current instanceof TimeoutException) {
				return true;
			}
			current = current.getCause();
		}
		return false;
	}
}
