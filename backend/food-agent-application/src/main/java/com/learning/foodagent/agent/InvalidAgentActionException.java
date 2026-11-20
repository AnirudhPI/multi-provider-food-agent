package com.learning.foodagent.agent;

public final class InvalidAgentActionException extends RuntimeException {

	public InvalidAgentActionException(String message) {
		super(message);
	}

	public InvalidAgentActionException(String message, Throwable cause) {
		super(message, cause);
	}
}
