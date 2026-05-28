package org.carey.masterchef.config;

import ai.z.openapi.ZhipuAiClient;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.EventListener;
import org.springframework.util.StringUtils;

/**
 * 智谱 AI 客户端配置。
 * <p>
 * 创建 {@link ZhipuAiClient} Bean，供 {@link org.carey.masterchef.service.ZhipuWebSearchService} 使用。
 * 仅当 application.yml 中配置了有效的 {@code zhipu.api-key} 时才加载。
 * </p>
 */
@Slf4j
@Configuration
@EnableConfigurationProperties(ZhipuProperties.class)
public class ZhipuConfig {

    private final ZhipuProperties properties;

    public ZhipuConfig(ZhipuProperties properties) {
        this.properties = properties;
    }

    /**
     * 构建智谱 AI 客户端。
     * API Key 格式为 {@code id.secret}，通过 {@link ZhipuApiKeySupport} 解析。
     */
    @Bean
    @Conditional(ZhipuApiKeyPresentCondition.class)
    public ZhipuAiClient zhipuAiClient() {
        ZhipuAiClient.Builder builder = ZhipuAiClient.builder().ofZHIPU();
        if (StringUtils.hasText(properties.baseUrl())) {
            builder.baseUrl(properties.baseUrl());
        }
        builder.apiKey(ZhipuApiKeySupport.resolve(properties.apiKey()));
        return builder.build();
    }

    /** 启动时检查 Key 是否配置，未配置则打印警告指引 */
    @EventListener(ApplicationReadyEvent.class)
    public void logZhipuKeyStatus() {
        if (ZhipuApiKeySupport.resolve(properties.apiKey()) == null) {
            log.warn("""
                    智谱 WebSearch 未启用：未检测到有效 API Key。
                    请在 application.yml 配置 zhipu.api-key（格式 id.secret）。
                    获取地址：https://open.bigmodel.cn/usercenter/apikeys
                    """);
        }
    }
}
