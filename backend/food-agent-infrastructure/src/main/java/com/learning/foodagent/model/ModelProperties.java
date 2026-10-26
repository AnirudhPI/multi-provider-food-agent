package com.learning.foodagent.model;

import java.net.URI;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

@Validated
@ConfigurationProperties("food-agent.model")
public record ModelProperties(
		@NotNull Provider provider,
		@NotNull URI baseUrl,
		@NotBlank String model,
		@NotNull Duration timeout,
		@DecimalMin("0.0") @DecimalMax("2.0") double temperature,
		@Positive int contextWindow) {

	public ModelProperties {
		if (baseUrl != null && (!baseUrl.isAbsolute()
				|| !("http".equalsIgnoreCase(baseUrl.getScheme()) || "https".equalsIgnoreCase(baseUrl.getScheme()))
				|| baseUrl.getHost() == null
				|| baseUrl.getHost().isBlank())) {
			throw new IllegalArgumentException("Model base URL must be an absolute HTTP or HTTPS URL");
		}
		if (timeout != null && (timeout.isZero() || timeout.isNegative())) {
			throw new IllegalArgumentException("Model timeout must be greater than zero");
		}
	}

	public enum Provider {
		OLLAMA,
		FAKE
	}
}
