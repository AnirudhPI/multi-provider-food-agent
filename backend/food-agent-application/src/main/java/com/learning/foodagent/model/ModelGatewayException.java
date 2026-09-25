package com.learning.foodagent.model;

public sealed class ModelGatewayException extends RuntimeException
		permits InvalidModelResponseException, ModelTimeoutException, ModelUnavailableException {

	ModelGatewayException(String message) {
		super(message);
	}

	ModelGatewayException(String message, Throwable cause) {
		super(message, cause);
	}
}
