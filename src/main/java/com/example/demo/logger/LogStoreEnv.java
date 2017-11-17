package com.example.demo.logger;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@ConfigurationProperties(prefix = "azure.log.store")
@Component
public @Data class LogStoreEnv {
	private String azureSharedKey;
	private String azureWid;
	private String appName;
	private String appVersion;

	// log manual
	@Value("true")
	private Boolean payload;
}
