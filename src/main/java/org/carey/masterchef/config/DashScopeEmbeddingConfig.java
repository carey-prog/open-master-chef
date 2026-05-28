package org.carey.masterchef.config;

import com.alibaba.cloud.ai.dashscope.api.DashScopeApi;
import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingModel;
import com.alibaba.cloud.ai.dashscope.embedding.DashScopeEmbeddingOptions;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Conditional;
import org.springframework.context.annotation.Configuration;

/**
 * 阿里云百炼 DashScope Embedding 模型配置。
 * <p>
 * Embedding 模型负责将文本转为高维向量（本项目的 text-embedding-v3 输出 1024 维），
 * 向量之间的余弦相似度可用于语义检索——即 RAG 的核心能力。
 * </p>
 *
 * <p>仅当 {@code spring.ai.dashscope.api-key} 有效时才加载（见 {@link DashScopeApiKeyPresentCondition}）。</p>
 */
@Slf4j
@Configuration
@Conditional(DashScopeApiKeyPresentCondition.class)
public class DashScopeEmbeddingConfig {

    /**
     * 创建 DashScope EmbeddingModel Bean。
     * <p>
     * {@code MetadataMode.EMBED} 表示只将文档正文内容用于向量化，忽略 metadata 字段。
     * {@code @ConditionalOnMissingBean} 避免与其他 Embedding 自动配置冲突。
     * </p>
     */
    @Bean
    @ConditionalOnMissingBean(EmbeddingModel.class)
    public EmbeddingModel dashScopeEmbeddingModel(
            @Value("${spring.ai.dashscope.api-key}") String apiKey,
            @Value("${spring.ai.dashscope.embedding.options.model:text-embedding-v3}") String model) {
        log.info("初始化 DashScope EmbeddingModel，model={}", model);
        DashScopeApi dashScopeApi = DashScopeApi.builder().apiKey(apiKey).build();
        DashScopeEmbeddingOptions options = DashScopeEmbeddingOptions.builder()
                .withModel(model)
                .build();
        return new DashScopeEmbeddingModel(dashScopeApi, MetadataMode.EMBED, options);
    }
}
