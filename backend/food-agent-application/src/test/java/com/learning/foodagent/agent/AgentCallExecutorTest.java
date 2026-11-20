package com.learning.foodagent.agent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.time.Duration;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.LockSupport;

import org.junit.jupiter.api.Test;

class AgentCallExecutorTest {

	@Test
	void returnsCompletedOperation() {
		try (var executorService = Executors.newVirtualThreadPerTaskExecutor()) {
			var executor = new AgentCallExecutor(executorService);

			assertEquals("done", executor.execute(() -> "done", Duration.ofSeconds(1)));
		}
	}

	@Test
	void cancelsOperationAtRemainingTurnDeadline() {
		try (var executorService = Executors.newVirtualThreadPerTaskExecutor()) {
			var executor = new AgentCallExecutor(executorService);

			assertThrows(AgentTimeoutException.class, () -> executor.execute(() -> {
				while (!Thread.currentThread().isInterrupted()) {
					LockSupport.parkNanos(Duration.ofMillis(10).toNanos());
				}
				return "cancelled";
			}, Duration.ofMillis(20)));
		}
	}
}
