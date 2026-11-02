package com.learning.foodagent.discovery;

import com.learning.foodagent.domain.discovery.FoodProvider;
import com.learning.foodagent.domain.discovery.RestaurantSearchCriteria;
import com.learning.foodagent.domain.discovery.RestaurantSearchResult;

public interface RestaurantDiscoveryGateway {

	FoodProvider provider();

	RestaurantSearchResult search(RestaurantSearchCriteria criteria);
}
