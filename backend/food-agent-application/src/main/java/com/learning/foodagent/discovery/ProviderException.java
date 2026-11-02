package com.learning.foodagent.discovery;

import com.learning.foodagent.domain.discovery.FoodProvider;

public sealed class ProviderException extends RuntimeException
		permits ProviderAuthenticationException, ProviderResponseException,
		ProviderTimeoutException, ProviderUnavailableException {

	private final FoodProvider provider;

	ProviderException(FoodProvider provider, String message) {
		super(message);
		this.provider = provider;
	}

	ProviderException(FoodProvider provider, String message, Throwable cause) {
		super(message, cause);
		this.provider = provider;
	}

	public FoodProvider provider() {
		return provider;
	}
}
