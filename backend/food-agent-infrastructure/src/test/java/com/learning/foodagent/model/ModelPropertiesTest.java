package com.learning.foodagent.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.net.URI;
import java.time.Duration;

import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;

class ModelPropertiesTest {

	@Test
	void rejectsNonHttpBaseUrl() {
		assertThatIllegalArgumentException().isThrownBy(() -> properties(URI.create("file:///tmp/model"), Duration.ofSeconds(1)))
				.withMessage("Model base URL must be an absolute HTTP or HTTPS URL");
	}

	@Test
	void rejectsBaseUrlWithoutHost() {
		assertThatIllegalArgumentException().isThrownBy(() -> properties(URI.create("http:ollama"), Duration.ofSeconds(1)))
				.withMessage("Model base URL must be an absolute HTTP or HTTPS URL");
	}

	@Test
	void rejectsNonPositiveTimeout() {
		assertThatIllegalArgumentException().isThrownBy(() -> properties(URI.create("http://localhost:11434"), Duration.ZERO))
				.withMessage("Model timeout must be greater than zero");
	}

	@Test
	void validatesTemperatureAndContextWindowBounds() {
		try (var validatorFactory = Validation.buildDefaultValidatorFactory()) {
			var invalid = new ModelProperties(
					ModelProperties.Provider.OLLAMA,
					URI.create("http://localhost:11434"),
					"qwen3:8b",
					Duration.ofSeconds(1),
					2.1,
					0);

			assertThat(validatorFactory.getValidator().validate(invalid))
					.extracting(violation -> violation.getPropertyPath().toString())
					.containsExactlyInAnyOrder("temperature", "contextWindow");
		}
	}

	private ModelProperties properties(URI baseUrl, Duration timeout) {
		return new ModelProperties(
				ModelProperties.Provider.OLLAMA,
				baseUrl,
				"qwen3:8b",
				timeout,
				0.1,
				8192);
	}
}
