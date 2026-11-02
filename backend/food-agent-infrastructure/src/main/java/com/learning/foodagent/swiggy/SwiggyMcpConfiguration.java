package com.learning.foodagent.swiggy;

import java.time.Clock;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import io.modelcontextprotocol.client.transport.customizer.McpSyncHttpClientRequestCustomizer;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SwiggyProperties.class)
class SwiggyMcpConfiguration {

	@Bean
	Clock providerClock() {
		return Clock.systemUTC();
	}

	@Bean
	@ConditionalOnProperty(prefix = "food-agent.providers.swiggy", name = "enabled", havingValue = "true")
	McpSyncHttpClientRequestCustomizer swiggyAuthorizationHeader(SwiggyProperties properties) {
		return (request, method, uri, body, context) -> {
			if (!properties.accessToken().isBlank() && sameOrigin(properties.baseUrl(), uri)) {
				request.header("Authorization", "Bearer " + properties.accessToken());
			}
		};
	}

	private static boolean sameOrigin(java.net.URI configured, java.net.URI request) {
		return configured.getScheme().equalsIgnoreCase(request.getScheme())
				&& configured.getHost().equalsIgnoreCase(request.getHost())
				&& effectivePort(configured) == effectivePort(request);
	}

	private static int effectivePort(java.net.URI uri) {
		if (uri.getPort() >= 0) {
			return uri.getPort();
		}
		return "https".equalsIgnoreCase(uri.getScheme()) ? 443 : 80;
	}
}
