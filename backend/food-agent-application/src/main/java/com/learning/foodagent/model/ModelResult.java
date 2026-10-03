package com.learning.foodagent.model;

public record ModelResult(Category category, String message) {

	public enum Category {
		FOOD_REQUEST,
		CLARIFICATION,
		UNSUPPORTED
	}
}
