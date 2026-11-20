package com.learning.foodagent.agent;

import java.io.IOException;
import java.util.concurrent.Executor;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import jakarta.validation.Valid;

import com.learning.foodagent.discovery.ProviderAuthenticationException;
import com.learning.foodagent.discovery.ProviderResponseException;
import com.learning.foodagent.discovery.ProviderTimeoutException;
import com.learning.foodagent.discovery.ProviderUnavailableException;
import com.learning.foodagent.model.InvalidModelResponseException;
import com.learning.foodagent.model.ModelTimeoutException;
import com.learning.foodagent.model.ModelUnavailableException;

@RestController
@RequestMapping("/api/v1/agent/turns")
final class AgentController {

	private final AgentOrchestrator orchestrator;
	private final Executor executor;

	AgentController(AgentOrchestrator orchestrator, @Qualifier("agentExecutor") Executor executor) {
		this.orchestrator = orchestrator;
		this.executor = executor;
	}

	@PostMapping
	AgentTurnResponse create(@Valid @RequestBody AgentTurnRequest request) {
		return AgentTurnResponse.from(orchestrator.handle(request.toCommand()));
	}

	@PostMapping(value = "/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
	SseEmitter stream(@Valid @RequestBody AgentTurnRequest request) {
		SseEmitter emitter = new SseEmitter(100_000L);
		executor.execute(() -> executeStream(request, emitter));
		return emitter;
	}

	private void executeStream(AgentTurnRequest request, SseEmitter emitter) {
		try {
			AgentTurnResult result = orchestrator.handle(request.toCommand(),
					event -> send(emitter, "trace", AgentTurnResponse.TraceEventResponse.from(event)));
			send(emitter, "result", AgentTurnResponse.from(result));
			emitter.complete();
		}
		catch (RuntimeException exception) {
			try {
				send(emitter, "error", streamError(exception));
				emitter.complete();
			}
			catch (RuntimeException sendFailure) {
				emitter.completeWithError(sendFailure);
			}
		}
	}

	private void send(SseEmitter emitter, String eventName, Object data) {
		try {
			emitter.send(SseEmitter.event().name(eventName).data(data));
		}
		catch (IOException | IllegalStateException exception) {
			throw new AgentStreamException("Agent event stream is no longer available", exception);
		}
	}

	private AgentStreamErrorResponse streamError(RuntimeException exception) {
		return switch (exception) {
			case InvalidModelResponseException ignored -> error("INVALID_MODEL_RESPONSE", exception);
			case ModelTimeoutException ignored -> error("MODEL_TIMEOUT", exception);
			case ModelUnavailableException ignored -> error("MODEL_UNAVAILABLE", exception);
			case InvalidAgentActionException ignored -> error("INVALID_AGENT_ACTION", exception);
			case AgentStepLimitException ignored -> error("AGENT_STEP_LIMIT", exception);
			case AgentTimeoutException ignored -> error("AGENT_TIMEOUT", exception);
			case ProviderAuthenticationException ignored -> error("PROVIDER_NOT_AUTHENTICATED", exception);
			case ProviderTimeoutException ignored -> error("PROVIDER_TIMEOUT", exception);
			case ProviderResponseException ignored -> error("PROVIDER_RESPONSE_ERROR", exception);
			case ProviderUnavailableException ignored -> error("PROVIDER_UNAVAILABLE", exception);
			default -> error("AGENT_STREAM_ERROR", exception);
		};
	}

	private AgentStreamErrorResponse error(String code, RuntimeException exception) {
		String message = exception.getMessage() == null ? "Agent request failed" : exception.getMessage();
		return new AgentStreamErrorResponse(code, message);
	}

	private record AgentStreamErrorResponse(String code, String message) {
	}

	private static final class AgentStreamException extends RuntimeException {

		private AgentStreamException(String message, Throwable cause) {
			super(message, cause);
		}
	}
}
