package com.learning.foodagent.discovery;

import com.learning.foodagent.domain.discovery.FoodProvider;

public final class ProviderResponseException extends ProviderException {

	public ProviderResponseException(FoodProvider provider, String message) {
		super(provider, message);
	}

	public ProviderResponseException(FoodProvider provider, String message, Throwable cause) {
		super(provider, message, cause);
	}
}
