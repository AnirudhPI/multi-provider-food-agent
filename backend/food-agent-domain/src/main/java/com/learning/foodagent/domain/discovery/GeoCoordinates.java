package com.learning.foodagent.domain.discovery;

public record GeoCoordinates(double latitude, double longitude) {

	public GeoCoordinates {
		if (!Double.isFinite(latitude) || latitude < -90 || latitude > 90) {
			throw new IllegalArgumentException("latitude must be between -90 and 90");
		}
		if (!Double.isFinite(longitude) || longitude < -180 || longitude > 180) {
			throw new IllegalArgumentException("longitude must be between -180 and 180");
		}
	}
}
