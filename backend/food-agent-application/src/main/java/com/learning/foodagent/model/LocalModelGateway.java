package com.learning.foodagent.model;

public interface LocalModelGateway {

	ModelHealth health();

	ModelResult generate(ModelPrompt prompt);
}
