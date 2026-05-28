package org.carey.masterchef.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "zhipu")
public record ZhipuProperties(String apiKey, String baseUrl) {
}
