package org.carey.masterchef.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Data
@ConfigurationProperties(prefix = "masterchef.mimo")
public class MimoProperties {
    private String apiKey;
    private String baseUrl;
    private String visionModel;
    private String imageModel;
}
