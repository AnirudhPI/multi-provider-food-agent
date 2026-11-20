package com.learning.foodagent.agent;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/agent/turns")
final class AgentController {

	private final AgentOrchestrator orchestrator;

	AgentController(AgentOrchestrator orchestrator) {
		this.orchestrator = orchestrator;
	}

	@PostMapping
	AgentTurnResponse create(@Valid @RequestBody AgentTurnRequest request) {
		return AgentTurnResponse.from(orchestrator.handle(request.toCommand()));
	}
}
