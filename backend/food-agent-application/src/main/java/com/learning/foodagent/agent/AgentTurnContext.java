package com.learning.foodagent.agent;

import com.learning.foodagent.domain.discovery.GeoCoordinates;

public record AgentTurnContext(GeoCoordinates location) {
}
