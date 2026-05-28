package org.carey.masterchef.config;

import org.springframework.util.StringUtils;

public final class ZhipuApiKeySupport {

    private ZhipuApiKeySupport() {
    }

    public static boolean isValid(String apiKey) {
        if (!StringUtils.hasText(apiKey)) {
            return false;
        }
        return apiKey.trim().split("\\.", -1).length == 2;
    }

    public static String resolve(String configuredKey) {
        if (isValid(configuredKey)) {
            return configuredKey.trim();
        }
        String fromZhipuEnv = trimToNull(System.getenv("ZHIPU_API_KEY"));
        if (isValid(fromZhipuEnv)) {
            return fromZhipuEnv;
        }
        String fromZaiEnv = trimToNull(System.getenv("ZAI_API_KEY"));
        if (isValid(fromZaiEnv)) {
            return fromZaiEnv;
        }
        return null;
    }

    private static String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }
}
