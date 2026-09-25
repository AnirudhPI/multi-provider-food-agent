package com.learning.foodagent.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record GenerateModelRequest(
		@NotBlank(message = "prompt must not be blank")
		@Size(max = 4_000, message = "prompt must not exceed 4000 characters")
		String prompt) {
}
