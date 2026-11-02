package com.learning.foodagent.swiggy;

import java.net.URI;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("food-agent.providers.swiggy")
public final class SwiggyProperties {

	private final boolean enabled;
	private final URI baseUrl;
	private final String accessToken;

	public SwiggyProperties(boolean enabled, URI baseUrl, String accessToken) {
		this.enabled = enabled;
		this.baseUrl = baseUrl;
		this.accessToken = accessToken == null ? "" : accessToken.strip();
		if (baseUrl == null || !baseUrl.isAbsolute() || baseUrl.getHost() == null
				|| !("https".equalsIgnoreCase(baseUrl.getScheme()) || "http".equalsIgnoreCase(baseUrl.getScheme()))) {
			throw new IllegalArgumentException("Swiggy base URL must be an absolute HTTP or HTTPS URL");
		}
	}

	public boolean enabled() {
		return enabled;
	}

	public URI baseUrl() {
		return baseUrl;
	}

	public String accessToken() {
		return accessToken;
	}
}
