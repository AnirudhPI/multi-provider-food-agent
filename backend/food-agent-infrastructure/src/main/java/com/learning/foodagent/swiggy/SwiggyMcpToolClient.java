package com.learning.foodagent.swiggy;

import java.util.Map;

interface SwiggyMcpToolClient {

	ToolResult call(String toolName, Map<String, Object> arguments);

	record ToolResult(String textPayload, String providerMessage, boolean error) {
	}

	final class AuthenticationFailure extends RuntimeException {

		AuthenticationFailure(Throwable cause) {
			super(cause);
		}
	}
}
