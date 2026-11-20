package com.learning.foodagent.agent;

import java.time.Duration;
import java.util.Objects;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;

public final class AgentCallExecutor {

	private final ExecutorService executor;

	public AgentCallExecutor(ExecutorService executor) {
		this.executor = Objects.requireNonNull(executor, "executor must not be null");
	}

	public <T> T execute(Supplier<T> operation, Duration remaining) {
		if (remaining.isZero() || remaining.isNegative()) {
			throw new AgentTimeoutException("Agent turn exceeded its time limit");
		}

		Future<T> future = executor.submit(operation::get);
		try {
			return future.get(remaining.toNanos(), TimeUnit.NANOSECONDS);
		}
		catch (TimeoutException exception) {
			future.cancel(true);
			throw new AgentTimeoutException("Agent turn exceeded its time limit");
		}
		catch (InterruptedException exception) {
			future.cancel(true);
			Thread.currentThread().interrupt();
			throw new AgentTimeoutException("Agent turn was cancelled");
		}
		catch (ExecutionException exception) {
			if (exception.getCause() instanceof RuntimeException runtimeException) {
				throw runtimeException;
			}
			throw new IllegalStateException("Agent operation failed", exception.getCause());
		}
	}
}
