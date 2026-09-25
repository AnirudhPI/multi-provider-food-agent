package com.learning.foodagent.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import jakarta.validation.Validation;

class StructuredResponseDecoderTest {

	private final StructuredResponseDecoder decoder = new StructuredResponseDecoder(
			Validation.buildDefaultValidatorFactory().getValidator());

	@Test
	void decodesValidStructuredOutput() {
		ModelResult result = decoder.decode("""
				{"category":"FOOD_REQUEST","message":"The user wants dosa."}
				""");

		assertThat(result.category()).isEqualTo(ModelResult.Category.FOOD_REQUEST);
		assertThat(result.message()).isEqualTo("The user wants dosa.");
	}

	@Test
	void rejectsMalformedOutput() {
		assertThatThrownBy(() -> decoder.decode("not-json"))
				.isInstanceOf(InvalidModelResponseException.class)
				.hasMessage("Local model returned malformed structured output");
	}

	@Test
	void rejectsOutputThatViolatesConstraints() {
		assertThatThrownBy(() -> decoder.decode("""
				{"category":"FOOD_REQUEST","message":""}
				"""))
				.isInstanceOf(InvalidModelResponseException.class)
				.hasMessage("Local model response failed schema validation");
	}
}
