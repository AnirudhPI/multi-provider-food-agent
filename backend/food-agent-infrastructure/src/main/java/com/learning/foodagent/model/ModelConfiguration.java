package com.learning.foodagent.model;

import java.net.http.HttpClient;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.ai.chat.client.ChatClient;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(ModelProperties.class)
class ModelConfiguration {

	@Bean(destroyMethod = "close")
	ExecutorService modelExecutor() {
		return Executors.newVirtualThreadPerTaskExecutor();
	}

	@Bean
	ChatClient modelChatClient(ChatClient.Builder builder) {
		return builder.build();
	}

	@Bean
	RestClient ollamaRestClient(ModelProperties properties) {
		var httpClient = HttpClient.newBuilder()
				.connectTimeout(properties.timeout())
				.build();
		var requestFactory = new JdkClientHttpRequestFactory(httpClient);
		requestFactory.setReadTimeout(properties.timeout());

		return RestClient.builder()
				.baseUrl(properties.baseUrl().toString())
				.requestFactory(requestFactory)
				.build();
	}
}
