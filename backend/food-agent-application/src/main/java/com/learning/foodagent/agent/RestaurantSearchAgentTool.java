package com.learning.foodagent.agent;

import java.util.List;
import java.util.Map;
import java.util.Set;

import org.springframework.stereotype.Component;

import com.learning.foodagent.discovery.RestaurantDiscoveryService;
import com.learning.foodagent.domain.discovery.FoodProvider;
import com.learning.foodagent.domain.discovery.RestaurantSearchCriteria;
import com.learning.foodagent.domain.discovery.RestaurantSearchResult;
import com.learning.foodagent.domain.discovery.RestaurantSummary;

@Component
public final class RestaurantSearchAgentTool implements ReadOnlyAgentTool {

	public static final String NAME = "search_restaurants";

	private static final Set<String> ALLOWED_ARGUMENTS = Set.of("query");

	private final RestaurantDiscoveryService discoveryService;
	private final AgentLimits limits;

	public RestaurantSearchAgentTool(RestaurantDiscoveryService discoveryService, AgentLimits limits) {
		this.discoveryService = discoveryService;
		this.limits = limits;
	}

	@Override
	public String name() {
		return NAME;
	}

	@Override
	public AgentToolResult execute(Map<String, String> arguments, AgentTurnContext context) {
		if (!ALLOWED_ARGUMENTS.containsAll(arguments.keySet())) {
			throw new InvalidAgentActionException("Restaurant search contains unsupported arguments");
		}
		String query = arguments.get("query");
		if (query == null || query.isBlank()) {
			throw new InvalidAgentActionException("Restaurant search requires a query");
		}
		if (context.location() == null) {
			throw new InvalidAgentActionException("Restaurant search requires a resolved location");
		}

		final RestaurantSearchCriteria criteria;
		try {
			criteria = new RestaurantSearchCriteria(query, context.location(), null);
		}
		catch (IllegalArgumentException exception) {
			throw new InvalidAgentActionException("Restaurant search arguments are invalid", exception);
		}

		RestaurantSearchResult providerResult = discoveryService.search(FoodProvider.SWIGGY, criteria);
		RestaurantSearchResult boundedResult = limit(providerResult);
		return new AgentToolResult(observation(boundedResult), List.of(boundedResult));
	}

	private RestaurantSearchResult limit(RestaurantSearchResult result) {
		List<RestaurantSummary> restaurants = result.restaurants().stream()
				.limit(limits.maximumResultsPerTool())
				.toList();
		return new RestaurantSearchResult(
				result.provider(),
				result.query(),
				restaurants,
				null,
				result.hasMore() || result.restaurants().size() > restaurants.size(),
				result.observedAt());
	}

	private String observation(RestaurantSearchResult result) {
		StringBuilder observation = new StringBuilder()
				.append("provider=").append(result.provider()).append('\n')
				.append("query=").append(safe(result.query())).append('\n')
				.append("restaurant_count=").append(result.restaurants().size()).append('\n');

		for (int index = 0; index < result.restaurants().size(); index++) {
			RestaurantSummary restaurant = result.restaurants().get(index);
			observation.append("restaurant[").append(index).append("]={")
					.append("name=").append(safe(restaurant.name()))
					.append(", cuisines=").append(safe(String.join(", ", restaurant.cuisines())))
					.append(", rating=").append(value(restaurant.rating()))
					.append(", costForTwo=").append(safe(restaurant.costForTwoDisplay()))
					.append(", area=").append(safe(restaurant.areaName()))
					.append(", deliveryMinutes=").append(value(restaurant.deliveryTimeMinutes()))
					.append(", vegetarian=").append(value(restaurant.vegetarian()))
					.append("}\n");
		}
		return observation.toString().strip();
	}

	private String safe(String value) {
		if (value == null) {
			return "unknown";
		}
		return value.replace("\\", "\\\\")
				.replace("\n", " ")
				.replace("\r", " ")
				.replace("{", "(")
				.replace("}", ")");
	}

	private String value(Object value) {
		return value == null ? "unknown" : safe(value.toString());
	}
}
