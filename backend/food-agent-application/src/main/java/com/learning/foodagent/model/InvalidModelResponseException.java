package com.learning.foodagent.model;

public final class InvalidModelResponseException extends ModelGatewayException {

	public InvalidModelResponseException(String message) {
		super(message);
	}

	public InvalidModelResponseException(String message, Throwable cause) {
		super(message, cause);
	}
}
