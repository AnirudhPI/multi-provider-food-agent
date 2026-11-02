package com.learning.foodagent.discovery;

import com.learning.foodagent.domain.discovery.FoodProvider;

public final class ProviderAuthenticationException extends ProviderException {

	public ProviderAuthenticationException(FoodProvider provider, String message) {
		super(provider, message);
	}

	public ProviderAuthenticationException(FoodProvider provider, String message, Throwable cause) {
		super(provider, message, cause);
	}
}
