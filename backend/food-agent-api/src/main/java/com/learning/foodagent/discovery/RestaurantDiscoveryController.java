package com.learning.foodagent.discovery;

import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.learning.foodagent.domain.discovery.FoodProvider;
import com.learning.foodagent.domain.discovery.GeoCoordinates;
import com.learning.foodagent.domain.discovery.RestaurantSearchCriteria;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.Pattern;

@Validated
@RestController
@RequestMapping("/api/v1/providers/swiggy/restaurants")
public class RestaurantDiscoveryController {

	private final RestaurantDiscoveryService discoveryService;

	RestaurantDiscoveryController(RestaurantDiscoveryService discoveryService) {
		this.discoveryService = discoveryService;
	}

	@GetMapping
	RestaurantSearchResponse search(
			@RequestParam @NotBlank @Size(max = 200) String query,
			@RequestParam @DecimalMin("-90.0") @DecimalMax("90.0") double latitude,
			@RequestParam @DecimalMin("-180.0") @DecimalMax("180.0") double longitude,
			@RequestParam(required = false) @Size(max = 20) @Pattern(regexp = "[0-9]+") String continuationToken) {
		var criteria = new RestaurantSearchCriteria(
				query,
				new GeoCoordinates(latitude, longitude),
				continuationToken);
		return RestaurantSearchResponse.from(discoveryService.search(FoodProvider.SWIGGY, criteria));
	}
}
