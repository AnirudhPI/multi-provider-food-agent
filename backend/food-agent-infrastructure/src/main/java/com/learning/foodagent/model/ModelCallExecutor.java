package com.learning.foodagent.model;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.springframework.stereotype.Component;

@Component
final class ModelCallExecutor {

	private final ExecutorService executor;
	private final ModelProperties properties;

	ModelCallExecutor(ExecutorService modelExecutor, ModelProperties properties) {
		this.executor = modelExecutor;
		this.properties = properties;
	}

	<T> T execute(Callable<T> call) {
		Future<T> task = executor.submit(call);
		try {
			return task.get(properties.timeout().toMillis(), TimeUnit.MILLISECONDS);
		}
		catch (TimeoutException exception) {
			task.cancel(true);
			throw new ModelTimeoutException("Local model request timed out", exception);
		}
		catch (InterruptedException exception) {
			task.cancel(true);
			Thread.currentThread().interrupt();
			throw new ModelUnavailableException("Local model request was interrupted", exception);
		}
		catch (ExecutionException exception) {
			Throwable cause = exception.getCause();
			if (cause instanceof ModelGatewayException modelGatewayException) {
				throw modelGatewayException;
			}
			throw new ModelUnavailableException("Local model request failed", cause);
		}
	}
}
