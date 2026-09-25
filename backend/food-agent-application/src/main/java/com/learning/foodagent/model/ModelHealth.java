package com.learning.foodagent.model;

import java.time.Instant;

public record ModelHealth(Status status, String provider, String model, String detail, Instant checkedAt) {

	public enum Status {
		UP,
		DOWN
	}
}
