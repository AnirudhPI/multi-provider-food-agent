package com.learning.foodagent.swiggy;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.net.URI;
import java.net.http.HttpRequest;

import org.junit.jupiter.api.Test;

import io.modelcontextprotocol.common.McpTransportContext;

class SwiggyMcpConfigurationTest {

	private final SwiggyMcpConfiguration configuration = new SwiggyMcpConfiguration();

	@Test
	void attachesTokenOnlyToConfiguredSwiggyOrigin() {
		var properties = new SwiggyProperties(true, URI.create("https://mcp.swiggy.com"), "secret-token");
		var customizer = configuration.swiggyAuthorizationHeader(properties);

		var swiggyRequest = HttpRequest.newBuilder(URI.create("https://mcp.swiggy.com/food"));
		customizer.customize(
				swiggyRequest,
				"POST",
				URI.create("https://mcp.swiggy.com/food"),
				"{}",
				McpTransportContext.EMPTY);
		var otherRequest = HttpRequest.newBuilder(URI.create("https://mcp.other.example/mcp"));
		customizer.customize(
				otherRequest,
				"POST",
				URI.create("https://mcp.other.example/mcp"),
				"{}",
				McpTransportContext.EMPTY);

		assertEquals("Bearer secret-token", swiggyRequest.build().headers().firstValue("Authorization").orElseThrow());
		assertTrue(otherRequest.build().headers().firstValue("Authorization").isEmpty());
	}
}
