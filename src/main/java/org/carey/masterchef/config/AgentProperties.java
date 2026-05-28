package org.carey.masterchef.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "masterchef.agent")
public class AgentProperties {
    private int stateTtlHours = 24;
    private String sessionPrefix = "agent:session:";
}
