package com.learning.foodagent.agent;

import com.learning.foodagent.domain.discovery.GeoCoordinates;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

record AgentTurnRequest(
		@NotBlank @Size(max = 2_000) String message,
		@Valid LocationRequest location) {

	AgentTurnCommand toCommand() {
		GeoCoordinates coordinates = location == null
				? null
				: new GeoCoordinates(location.latitude(), location.longitude());
		return new AgentTurnCommand(message, coordinates);
	}

	record LocationRequest(
			@NotNull @DecimalMin("-90.0") @DecimalMax("90.0") Double latitude,
			@NotNull @DecimalMin("-180.0") @DecimalMax("180.0") Double longitude) {
	}
}
