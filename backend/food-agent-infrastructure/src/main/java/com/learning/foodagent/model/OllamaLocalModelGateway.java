package com.learning.foodagent.model;

import java.time.Instant;
import java.util.List;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
@ConditionalOnProperty(name = "food-agent.model.provider", havingValue = "ollama", matchIfMissing = true)
final class OllamaLocalModelGateway implements LocalModelGateway {

	private static final String SYSTEM_PROMPT = """
			You classify a user's message for a food-ordering assistant.
			Use FOOD_REQUEST for requests about food, restaurants, menus, delivery, or ordering.
			Use CLARIFICATION when the user is discussing food but essential meaning is missing.
			Use UNSUPPORTED for unrelated requests.
			Do not claim that a restaurant, menu item, price, or provider result exists.
			Return only data matching the supplied schema.
			""";

	private final ChatClient chatClient;
	private final RestClient ollamaRestClient;
	private final ModelProperties properties;
	private final StructuredResponseDecoder responseDecoder;
	private final ModelCallExecutor callExecutor;

	OllamaLocalModelGateway(
			ChatClient modelChatClient,
			RestClient ollamaRestClient,
			ModelProperties properties,
			StructuredResponseDecoder responseDecoder,
			ModelCallExecutor callExecutor) {
		this.chatClient = modelChatClient;
		this.ollamaRestClient = ollamaRestClient;
		this.properties = properties;
		this.responseDecoder = responseDecoder;
		this.callExecutor = callExecutor;
	}

	@Override
	public ModelHealth health() {
		try {
			OllamaTags response = ollamaRestClient.get().uri("/api/tags").retrieve().body(OllamaTags.class);
			boolean modelInstalled = response != null && response.models() != null
					&& response.models().stream().anyMatch(model -> properties.model().equals(model.name()));
			return new ModelHealth(
					modelInstalled ? ModelHealth.Status.UP : ModelHealth.Status.DOWN,
					"ollama",
					properties.model(),
					modelInstalled ? "READY" : "MODEL_NOT_INSTALLED",
					Instant.now());
		}
		catch (RuntimeException exception) {
			return new ModelHealth(
					ModelHealth.Status.DOWN,
					"ollama",
					properties.model(),
					"OLLAMA_UNREACHABLE",
					Instant.now());
		}
	}

	@Override
	public ModelResult generate(ModelPrompt prompt) {
		return callExecutor.execute(() -> invokeModel(prompt));
	}

	private ModelResult invokeModel(ModelPrompt prompt) {
		try {
			String content = chatClient.prompt()
					.system(SYSTEM_PROMPT + System.lineSeparator() + responseDecoder.format())
					.user(prompt.prompt())
					.call()
					.content();

			return responseDecoder.decode(content);
		}
		catch (ModelGatewayException exception) {
			throw exception;
		}
		catch (RuntimeException exception) {
			throw new ModelUnavailableException("Local model is unavailable", exception);
		}
	}

	private record OllamaTags(List<OllamaModel> models) {
	}

	private record OllamaModel(String name) {
	}
}
