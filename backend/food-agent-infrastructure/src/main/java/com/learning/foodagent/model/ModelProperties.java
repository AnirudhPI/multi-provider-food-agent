package com.learning.foodagent.model;

import java.net.URI;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

@Validated
@ConfigurationProperties("food-agent.model")
public record ModelProperties(
		@NotBlank String provider,
		@NotNull URI baseUrl,
		@NotBlank String model,
		@NotNull Duration timeout) {
}
