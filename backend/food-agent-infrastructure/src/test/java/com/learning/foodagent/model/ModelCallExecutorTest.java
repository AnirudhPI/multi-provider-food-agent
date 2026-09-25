package com.learning.foodagent.model;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.Executors;

import org.junit.jupiter.api.Test;

class ModelCallExecutorTest {

	@Test
	void returnsCompletedModelCall() {
		try (var executorService = Executors.newVirtualThreadPerTaskExecutor()) {
			var executor = new ModelCallExecutor(executorService, properties(Duration.ofSeconds(1)));

			assertThat(executor.execute(() -> "ok")).isEqualTo("ok");
		}
	}

	@Test
	void cancelsModelCallWhenTimeoutExpires() {
		try (var executorService = Executors.newVirtualThreadPerTaskExecutor()) {
			var executor = new ModelCallExecutor(executorService, properties(Duration.ofMillis(20)));

			assertThatThrownBy(() -> executor.execute(() -> {
				Thread.sleep(Duration.ofSeconds(1));
				return "late";
			}))
					.isInstanceOf(ModelTimeoutException.class)
					.hasMessage("Local model request timed out");
		}
	}

	private ModelProperties properties(Duration timeout) {
		return new ModelProperties("ollama", URI.create("http://localhost:11434"), "qwen3:8b", timeout);
	}
}
