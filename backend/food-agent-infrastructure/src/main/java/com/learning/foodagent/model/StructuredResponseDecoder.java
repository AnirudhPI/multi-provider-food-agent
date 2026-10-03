package com.learning.foodagent.model;

import java.util.Set;

import org.springframework.ai.converter.BeanOutputConverter;
import org.springframework.stereotype.Component;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

@Component
final class StructuredResponseDecoder {

	private final Validator validator;
	private final BeanOutputConverter<OllamaStructuredResponse> outputConverter =
			new BeanOutputConverter<>(OllamaStructuredResponse.class);

	StructuredResponseDecoder(Validator validator) {
		this.validator = validator;
	}

	String format() {
		return outputConverter.getFormat();
	}

	ModelResult decode(String content) {
		final OllamaStructuredResponse response;
		try {
			response = outputConverter.convert(content);
		}
		catch (RuntimeException exception) {
			throw new InvalidModelResponseException("Local model returned malformed structured output", exception);
		}

		if (response == null) {
			throw new InvalidModelResponseException("Local model returned an empty structured response");
		}

		Set<ConstraintViolation<OllamaStructuredResponse>> violations = validator.validate(response);
		if (!violations.isEmpty()) {
			throw new InvalidModelResponseException("Local model response failed schema validation");
		}
		return new ModelResult(response.category(), response.message());
	}

	private record OllamaStructuredResponse(
			@jakarta.validation.constraints.NotNull ModelResult.Category category,
			@jakarta.validation.constraints.NotBlank
			@jakarta.validation.constraints.Size(max = 2_000) String message) {
	}
}
