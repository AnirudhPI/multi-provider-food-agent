package com.learning.foodagent.domain.discovery;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

class GeoCoordinatesTest {

	@Test
	void acceptsBoundaryCoordinates() {
		assertDoesNotThrow(() -> new GeoCoordinates(-90, 180));
	}

	@Test
	void rejectsCoordinatesOutsideGeographicBounds() {
		assertThrows(IllegalArgumentException.class, () -> new GeoCoordinates(90.1, 77));
		assertThrows(IllegalArgumentException.class, () -> new GeoCoordinates(12, -180.1));
		assertThrows(IllegalArgumentException.class, () -> new GeoCoordinates(Double.NaN, 77));
	}
}
