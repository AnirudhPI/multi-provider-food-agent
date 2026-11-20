package com.learning.foodagent.agent;

import java.util.Map;

public interface ReadOnlyAgentTool {

	String name();

	AgentToolResult execute(Map<String, String> arguments, AgentTurnContext context);
}
