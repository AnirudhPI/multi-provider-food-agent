package com.learning.foodagent.discovery;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

import com.learning.foodagent.domain.discovery.FoodProvider;
import com.learning.foodagent.domain.discovery.RestaurantSearchCriteria;
import com.learning.foodagent.domain.discovery.RestaurantSearchResult;

@Service
public final class RestaurantDiscoveryService {

	private final Map<FoodProvider, RestaurantDiscoveryGateway> gateways;

	public RestaurantDiscoveryService(List<RestaurantDiscoveryGateway> gateways) {
		this.gateways = new EnumMap<>(FoodProvider.class);
		for (RestaurantDiscoveryGateway gateway : gateways) {
			RestaurantDiscoveryGateway previous = this.gateways.put(gateway.provider(), gateway);
			if (previous != null) {
				throw new IllegalStateException("Multiple restaurant gateways configured for " + gateway.provider());
			}
		}
	}

	public RestaurantSearchResult search(FoodProvider provider, RestaurantSearchCriteria criteria) {
		RestaurantDiscoveryGateway gateway = gateways.get(provider);
		if (gateway == null) {
			throw new ProviderUnavailableException(provider, "Provider is not configured");
		}
		return gateway.search(criteria);
	}
}
