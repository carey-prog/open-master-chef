package org.carey.masterchef.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.SimpleLoggerAdvisor;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Duration;

/**
 * AI 相关 Bean 配置：DeepSeek 大模型 + JSON 序列化工具。
 * <p>
 * DeepSeek API 兼容 OpenAI 接口格式，因此使用 Spring AI 的 {@link OpenAiChatModel}
 * 配合自定义 baseUrl 和 apiKey 即可接入，无需单独的 DeepSeek SDK。
 * </p>
 *
 * <p>注意：{@code spring.ai.model.chat=none} 禁用了 Spring AI 的自动 ChatModel 配置，
 * 避免与 OpenAI 默认配置冲突；本类手动创建 DeepSeek 专用的 ChatModel Bean。</p>
 */
@Configuration
@EnableConfigurationProperties({DeepSeekProperties.class, MimoProperties.class})
public class AiConfig {

    /**
     * 创建 DeepSeek ChatModel（标记 @Primary 作为默认 ChatModel）。
     * <p>
     * 配置了连接超时 15 秒、读取超时 120 秒，因为大模型生成长文本可能较慢。
     * </p>
     */
    @Bean
    @Primary
    public ChatModel deepSeekChatModel(DeepSeekProperties props) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(15));
        requestFactory.setReadTimeout(Duration.ofSeconds(120));

        RestClient.Builder restClientBuilder = RestClient.builder().requestFactory(requestFactory);

        // OpenAiApi 是通用 OpenAI 兼容客户端，通过 baseUrl 指向 DeepSeek
        OpenAiApi openAiApi = OpenAiApi.builder()
                .restClientBuilder(restClientBuilder)
                .baseUrl(props.getBaseUrl())
                .apiKey(props.getApiKey())
                .build();
        OpenAiChatOptions options = OpenAiChatOptions.builder()
                .model(props.getModel())
                .temperature(props.getTemperature())
                .build();
        return OpenAiChatModel.builder()
                .openAiApi(openAiApi)
                .defaultOptions(options)
                .build();
    }

    /**
     * ChatClient 是 Spring AI 提供的高层 API，比直接调用 ChatModel 更简洁。
     * {@link org.carey.masterchef.service.LlmRecipeService} 通过它发送 Prompt 并获取回复。
     */
    @Bean
    @Primary
    public ChatClient deepSeekChatClient(ChatModel deepSeekChatModel) {
        return ChatClient.builder(deepSeekChatModel)
                .defaultAdvisors(new SimpleLoggerAdvisor()) // 自动打印请求/响应日志，便于调试
                .build();
    }

    /**
     * 全局 JSON 工具，注册 JavaTimeModule 以支持 LocalDateTime 等 Java 8 时间类型序列化。
     */
    @Bean
    public ObjectMapper objectMapper() {
        ObjectMapper mapper = new ObjectMapper();
        mapper.registerModule(new JavaTimeModule());
        return mapper;
    }
}
