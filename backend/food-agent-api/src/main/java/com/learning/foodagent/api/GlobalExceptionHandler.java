package com.learning.foodagent.api;

import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.learning.foodagent.model.InvalidModelResponseException;
import com.learning.foodagent.model.ModelTimeoutException;
import com.learning.foodagent.model.ModelUnavailableException;
import com.learning.foodagent.agent.AgentStepLimitException;
import com.learning.foodagent.agent.AgentTimeoutException;
import com.learning.foodagent.agent.InvalidAgentActionException;
import com.learning.foodagent.discovery.ProviderAuthenticationException;
import com.learning.foodagent.discovery.ProviderResponseException;
import com.learning.foodagent.discovery.ProviderTimeoutException;
import com.learning.foodagent.discovery.ProviderUnavailableException;

import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
final class GlobalExceptionHandler {

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ApiError> invalidRequest(MethodArgumentNotValidException exception) {
		return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Request validation failed");
	}

	@ExceptionHandler(InvalidModelResponseException.class)
	ResponseEntity<ApiError> invalidModelResponse(InvalidModelResponseException exception) {
		return error(HttpStatus.BAD_GATEWAY, "INVALID_MODEL_RESPONSE", exception.getMessage());
	}

	@ExceptionHandler(ModelTimeoutException.class)
	ResponseEntity<ApiError> modelTimeout(ModelTimeoutException exception) {
		return error(HttpStatus.GATEWAY_TIMEOUT, "MODEL_TIMEOUT", exception.getMessage());
	}

	@ExceptionHandler(ModelUnavailableException.class)
	ResponseEntity<ApiError> modelUnavailable(ModelUnavailableException exception) {
		return error(HttpStatus.SERVICE_UNAVAILABLE, "MODEL_UNAVAILABLE", exception.getMessage());
	}

	@ExceptionHandler(InvalidAgentActionException.class)
	ResponseEntity<ApiError> invalidAgentAction(InvalidAgentActionException exception) {
		return error(HttpStatus.BAD_GATEWAY, "INVALID_AGENT_ACTION", exception.getMessage());
	}

	@ExceptionHandler(AgentStepLimitException.class)
	ResponseEntity<ApiError> agentStepLimit(AgentStepLimitException exception) {
		return error(HttpStatus.BAD_GATEWAY, "AGENT_STEP_LIMIT", exception.getMessage());
	}

	@ExceptionHandler(AgentTimeoutException.class)
	ResponseEntity<ApiError> agentTimeout(AgentTimeoutException exception) {
		return error(HttpStatus.GATEWAY_TIMEOUT, "AGENT_TIMEOUT", exception.getMessage());
	}

	@ExceptionHandler(ConstraintViolationException.class)
	ResponseEntity<ApiError> invalidConstraint(ConstraintViolationException exception) {
		return error(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Request validation failed");
	}

	@ExceptionHandler(ProviderAuthenticationException.class)
	ResponseEntity<ApiError> providerAuthentication(ProviderAuthenticationException exception) {
		return error(HttpStatus.UNAUTHORIZED, "PROVIDER_NOT_AUTHENTICATED", exception.getMessage());
	}

	@ExceptionHandler(ProviderTimeoutException.class)
	ResponseEntity<ApiError> providerTimeout(ProviderTimeoutException exception) {
		return error(HttpStatus.GATEWAY_TIMEOUT, "PROVIDER_TIMEOUT", exception.getMessage());
	}

	@ExceptionHandler(ProviderResponseException.class)
	ResponseEntity<ApiError> providerResponse(ProviderResponseException exception) {
		return error(HttpStatus.BAD_GATEWAY, "PROVIDER_RESPONSE_ERROR", exception.getMessage());
	}

	@ExceptionHandler(ProviderUnavailableException.class)
	ResponseEntity<ApiError> providerUnavailable(ProviderUnavailableException exception) {
		return error(HttpStatus.SERVICE_UNAVAILABLE, "PROVIDER_UNAVAILABLE", exception.getMessage());
	}

	private ResponseEntity<ApiError> error(HttpStatus status, String code, String message) {
		return ResponseEntity.status(status).body(new ApiError(code, message, Instant.now()));
	}
}
