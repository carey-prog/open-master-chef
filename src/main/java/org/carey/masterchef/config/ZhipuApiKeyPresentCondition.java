package org.carey.masterchef.config;

import org.springframework.context.annotation.Condition;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

public class ZhipuApiKeyPresentCondition implements Condition {

    @Override
    public boolean matches(ConditionContext context, AnnotatedTypeMetadata metadata) {
        String fromConfig = context.getEnvironment().getProperty("zhipu.api-key");
        return ZhipuApiKeySupport.isValid(fromConfig)
                || ZhipuApiKeySupport.isValid(System.getenv("ZHIPU_API_KEY"))
                || ZhipuApiKeySupport.isValid(System.getenv("ZAI_API_KEY"));
    }
}
