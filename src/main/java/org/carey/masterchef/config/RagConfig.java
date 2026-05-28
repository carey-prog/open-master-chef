package org.carey.masterchef.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.redis.RedisVectorStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.boot.autoconfigure.data.redis.RedisProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import redis.clients.jedis.HostAndPort;
import redis.clients.jedis.JedisPooled;

/**
 * RAG 向量存储配置：将 DashScope Embedding 与 Redis Stack 向量索引连接。
 * <p>
 * <b>重要：</b>必须使用 Redis Stack（带 RediSearch 模块），普通 Redis 无法创建向量索引。
 * Docker 启动示例：{@code docker run -d -p 6379:6379 redis/redis-stack-server}
 * </p>
 *
 * <p>向量库工作流程：</p>
 * <ol>
 *   <li>启动时 {@link org.carey.masterchef.service.RagService} 读取 markdown 知识库</li>
 *   <li>DashScope EmbeddingModel 将文本转为向量</li>
 *   <li>RedisVectorStore 将向量和原文存入 RediSearch 索引</li>
 *   <li>检索时用 query 向量做相似度搜索，返回最相关的文档片段</li>
 * </ol>
 */
@Slf4j
@Configuration
public class RagConfig {

    /**
     * Jedis 连接池，用于与 Redis Stack 通信。
     * 连接参数来自 {@code spring.data.redis.*} 配置。
     */
    @Bean
    public JedisPooled jedisPooled(RedisProperties redisProperties) {
        return new JedisPooled(new HostAndPort(redisProperties.getHost(), redisProperties.getPort()));
    }

    /**
     * 创建 Redis 向量存储 Bean。
     * <p>
     * {@code @ConditionalOnBean(EmbeddingModel.class)} 表示只有 DashScope Embedding 就绪时才创建，
     * 避免 API Key 缺失时启动失败。
     * </p>
     */
    @Bean
    @ConditionalOnBean(EmbeddingModel.class)
    public VectorStore vectorStore(JedisPooled jedisPooled,
                                   EmbeddingModel embeddingModel,
                                   @Value("${spring.ai.vectorstore.redis.index-name:master-chef-rag}") String indexName,
                                   @Value("${spring.ai.vectorstore.redis.prefix:rag:}") String prefix) {
        log.info("初始化 Redis Stack VectorStore，index={}, prefix={}", indexName, prefix);
        return RedisVectorStore.builder(jedisPooled, embeddingModel)
                .indexName(indexName)       // RediSearch 索引名称
                .prefix(prefix)             // Redis key 前缀，如 rag:doc:xxx
                .initializeSchema(true)     // 首次启动自动创建向量索引 schema
                .build();
    }
}
