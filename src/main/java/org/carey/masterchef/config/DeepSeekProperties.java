package org.carey.masterchef.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "masterchef.deepseek")
public class DeepSeekProperties {
    private String apiKey;
    private String baseUrl;
    private String model;
    private Double temperature;
}
