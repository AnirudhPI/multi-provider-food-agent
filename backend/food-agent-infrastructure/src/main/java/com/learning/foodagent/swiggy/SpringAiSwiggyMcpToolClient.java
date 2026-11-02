package com.learning.foodagent.swiggy;

import java.util.List;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

import com.learning.foodagent.discovery.ProviderResponseException;
import com.learning.foodagent.domain.discovery.FoodProvider;

import io.modelcontextprotocol.client.McpSyncClient;
import io.modelcontextprotocol.client.transport.McpHttpClientTransportAuthorizationException;
import io.modelcontextprotocol.spec.McpError;
import io.modelcontextprotocol.spec.McpSchema;
import tools.jackson.databind.ObjectMapper;

@Component
@ConditionalOnProperty(prefix = "food-agent.providers.swiggy", name = "enabled", havingValue = "true")
final class SpringAiSwiggyMcpToolClient implements SwiggyMcpToolClient {

	private final McpSyncClient client;
	private final ObjectMapper objectMapper;

	SpringAiSwiggyMcpToolClient(List<McpSyncClient> clients, ObjectMapper objectMapper) {
		if (clients.size() != 1) {
			throw new IllegalStateException("Exactly one Swiggy MCP connection must be configured");
		}
		this.client = clients.getFirst();
		this.objectMapper = objectMapper;
	}

	@Override
	public ToolResult call(String toolName, Map<String, Object> arguments) {
		try {
			if (!client.isInitialized()) {
				client.initialize();
			}
			McpSchema.CallToolResult result = client.callTool(McpSchema.CallToolRequest.builder(toolName)
					.arguments(arguments)
					.build());
			String payload = payload(result, objectMapper);
			return new ToolResult(
					payload,
					diagnosticText(result, objectMapper),
					Boolean.TRUE.equals(result.isError()));
		}
		catch (McpHttpClientTransportAuthorizationException exception) {
			throw new AuthenticationFailure(exception);
		}
		catch (McpError exception) {
			if (isAuthenticationFailure(exception)) {
				throw new AuthenticationFailure(exception);
			}
			throw exception;
		}
		catch (RuntimeException exception) {
			if (isAuthenticationFailure(exception)) {
				throw new AuthenticationFailure(exception);
			}
			throw exception;
		}
	}

	static String payload(McpSchema.CallToolResult result, ObjectMapper objectMapper) {
		if (result.structuredContent() != null) {
			return objectMapper.writeValueAsString(result.structuredContent());
		}

		List<String> textPayloads = result.content().stream()
				.filter(McpSchema.TextContent.class::isInstance)
				.map(McpSchema.TextContent.class::cast)
				.map(McpSchema.TextContent::text)
				.filter(text -> text != null && !text.isBlank())
				.toList();
		if (textPayloads.isEmpty()) {
			throw invalidResponse();
		}
		return textPayloads.stream()
				.filter(text -> isJson(text, objectMapper))
				.findFirst()
				.orElse(textPayloads.getFirst());
	}

	private static boolean isJson(String payload, ObjectMapper objectMapper) {
		try {
			objectMapper.readTree(payload);
			return true;
		}
		catch (RuntimeException exception) {
			return false;
		}
	}

	static String diagnosticText(McpSchema.CallToolResult result, ObjectMapper objectMapper) {
		return result.content().stream()
				.filter(McpSchema.TextContent.class::isInstance)
				.map(McpSchema.TextContent.class::cast)
				.map(McpSchema.TextContent::text)
				.filter(text -> text != null && !text.isBlank())
				.filter(text -> !isJson(text, objectMapper))
				.findFirst()
				.orElse(null);
	}

	private static ProviderResponseException invalidResponse() {
		return new ProviderResponseException(FoodProvider.SWIGGY, "Swiggy returned an invalid restaurant response");
	}

	private static boolean isAuthenticationFailure(Throwable exception) {
		Throwable current = exception;
		while (current != null) {
			if (current instanceof McpHttpClientTransportAuthorizationException) {
				return true;
			}
			if (current instanceof McpError mcpError
					&& mcpError.getJsonRpcError() != null
					&& Integer.valueOf(-32001).equals(mcpError.getJsonRpcError().code())) {
				return true;
			}
			current = current.getCause();
		}
		return false;
	}
}
