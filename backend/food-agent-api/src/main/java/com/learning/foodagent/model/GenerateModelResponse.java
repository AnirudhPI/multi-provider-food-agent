package com.learning.foodagent.model;

record GenerateModelResponse(ModelResult.Category category, String message) {

	static GenerateModelResponse from(ModelResult result) {
		return new GenerateModelResponse(result.category(), result.message());
	}
}
