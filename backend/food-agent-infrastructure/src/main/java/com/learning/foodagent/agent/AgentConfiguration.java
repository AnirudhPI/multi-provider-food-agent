package com.learning.foodagent.agent;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(AgentProperties.class)
class AgentConfiguration {

	@Bean
	AgentLimits agentLimits(AgentProperties properties) {
		return new AgentLimits(
				properties.maximumSteps(),
				properties.maximumObservationCharacters(),
				properties.maximumResultsPerTool(),
				properties.timeout());
	}

	@Bean(destroyMethod = "close")
	ExecutorService agentExecutor() {
		return Executors.newVirtualThreadPerTaskExecutor();
	}

	@Bean
	AgentCallExecutor agentCallExecutor(@Qualifier("agentExecutor") ExecutorService agentExecutor) {
		return new AgentCallExecutor(agentExecutor);
	}
}
