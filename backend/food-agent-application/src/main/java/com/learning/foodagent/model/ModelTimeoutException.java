package com.learning.foodagent.model;

public final class ModelTimeoutException extends ModelGatewayException {

	public ModelTimeoutException(String message, Throwable cause) {
		super(message, cause);
	}
}
