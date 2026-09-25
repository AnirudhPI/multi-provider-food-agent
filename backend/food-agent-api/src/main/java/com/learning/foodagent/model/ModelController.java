package com.learning.foodagent.model;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/model")
final class ModelController {

	private final LocalModelGateway modelGateway;

	ModelController(LocalModelGateway modelGateway) {
		this.modelGateway = modelGateway;
	}

	@GetMapping("/health")
	ResponseEntity<ModelHealth> health() {
		ModelHealth health = modelGateway.health();
		return health.status() == ModelHealth.Status.UP
				? ResponseEntity.ok(health)
				: ResponseEntity.status(503).body(health);
	}

	@PostMapping("/generate")
	GenerateModelResponse generate(@Valid @RequestBody GenerateModelRequest request) {
		return GenerateModelResponse.from(modelGateway.generate(new ModelPrompt(request.prompt())));
	}
}
