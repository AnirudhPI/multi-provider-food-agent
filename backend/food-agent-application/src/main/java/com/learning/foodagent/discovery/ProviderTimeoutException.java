package com.learning.foodagent.discovery;

import com.learning.foodagent.domain.discovery.FoodProvider;

public final class ProviderTimeoutException extends ProviderException {

	public ProviderTimeoutException(FoodProvider provider, String message, Throwable cause) {
		super(provider, message, cause);
	}
}
