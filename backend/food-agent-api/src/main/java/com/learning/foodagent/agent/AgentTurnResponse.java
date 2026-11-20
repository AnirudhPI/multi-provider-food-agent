package com.learning.foodagent.agent;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import com.learning.foodagent.discovery.RestaurantSearchResponse;

record AgentTurnResponse(
		UUID turnId,
		String outcome,
		String message,
		List<RestaurantSearchResponse> restaurantSearches,
		List<TraceEventResponse> trace) {

	static AgentTurnResponse from(AgentTurnResult result) {
		return new AgentTurnResponse(
				result.turnId(),
				result.outcome().name(),
				result.message(),
				result.restaurantSearches().stream().map(RestaurantSearchResponse::from).toList(),
				result.trace().stream().map(TraceEventResponse::from).toList());
	}

	record TraceEventResponse(UUID turnId, String type, int step, String toolName, Instant occurredAt) {

		static TraceEventResponse from(AgentTraceEvent event) {
			return new TraceEventResponse(
					event.turnId(), event.type().name(), event.step(), event.toolName(), event.occurredAt());
		}
	}
}
