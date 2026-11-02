package com.learning.foodagent.discovery;

import com.learning.foodagent.domain.discovery.FoodProvider;

public final class ProviderUnavailableException extends ProviderException {

	public ProviderUnavailableException(FoodProvider provider, String message) {
		super(provider, message);
	}

	public ProviderUnavailableException(FoodProvider provider, String message, Throwable cause) {
		super(provider, message, cause);
	}
}
