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

	private ResponseEntity<ApiError> error(HttpStatus status, String code, String message) {
		return ResponseEntity.status(status).body(new ApiError(code, message, Instant.now()));
	}
}
