package org.carey.masterchef.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.carey.masterchef.config.DeepSeekProperties;
import org.springframework.ai.embedding.EmbeddingModel;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * AI 能力诊断服务：汇总各 AI 组件的启用状态。
 * <p>
 * 启动时打印诊断日志，同时通过 {@code GET /api/system/capabilities} 对外暴露，
 * 方便排查「DeepSeek 没调用」「RAG 没生效」等问题。
 * </p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AiCapabilityService {

    private final DeepSeekProperties deepSeekProperties;
    private final ObjectProvider<EmbeddingModel> embeddingModelProvider;
    private final ObjectProvider<VectorStore> vectorStoreProvider;
    private final RagService ragService;

    /** 应用启动完成后打印 AI 能力摘要到日志 */
    @EventListener(ApplicationReadyEvent.class)
    @Order(100)
    public void logCapabilities() {
        Map<String, Object> status = getStatus();
        log.info("AI 能力状态: DeepSeek={}, RAG={}, Embedding={}",
                status.get("deepseekEnabled"), status.get("ragEnabled"), status.get("embeddingEnabled"));
        if (!(Boolean) status.get("ragEnabled")) {
            log.warn("RAG 未启用，请检查: 1) spring.ai.dashscope.api-key  2) Redis Stack(需 RediSearch)  3) 端口是否正确");
        }
    }

    /**
     * 构建完整的 AI 能力状态 Map。
     * ragEnabled = VectorStore 存在 AND 知识库已成功加载
     */
    public Map<String, Object> getStatus() {
        Map<String, Object> status = new LinkedHashMap<>();
        boolean deepseekEnabled = StringUtils.hasText(deepSeekProperties.getApiKey())
                && !deepSeekProperties.getApiKey().startsWith("your-");
        boolean embeddingEnabled = embeddingModelProvider.getIfAvailable() != null;
        boolean ragEnabled = vectorStoreProvider.getIfAvailable() != null && ragService.isKnowledgeLoaded();

        status.put("deepseekEnabled", deepseekEnabled);
        status.put("deepseekModel", deepSeekProperties.getModel());
        status.put("deepseekBaseUrl", deepSeekProperties.getBaseUrl());
        status.put("embeddingEnabled", embeddingEnabled);
        status.put("embeddingModel", "text-embedding-v3");
        status.put("ragEnabled", ragEnabled);
        status.put("ragKnowledgeLoaded", ragService.isKnowledgeLoaded());
        status.put("workflow", Map.of(
                "ragNode", "RagSearchNode → DashScope向量检索烹饪知识库",
                "recipeNode", "RecipeGenerateNode → DeepSeek(" + deepSeekProperties.getModel() + ") 生成菜谱",
                "nutritionNode", "NutritionAnalysisNode → DeepSeek 营养分析"
        ));
        return status;
    }
}
