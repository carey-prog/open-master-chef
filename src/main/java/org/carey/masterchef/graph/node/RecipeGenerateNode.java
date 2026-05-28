package org.carey.masterchef.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.carey.masterchef.domain.dto.GeneratedRecipePayload;
import org.carey.masterchef.graph.RecipeGraphStateKeys;
import org.carey.masterchef.service.AgentStateService;
import org.carey.masterchef.service.LlmRecipeService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Graph 工作流第 3 步：DeepSeek 大模型生成菜谱节点。
 * <p>
 * 这是 Agent 的「核心创作」环节：将用户食材、菜系、自定义要求，
 * 以及前两步收集的 RAG 知识 + 联网搜索结果，一并交给 DeepSeek 生成结构化菜谱 JSON。
 * </p>
 *
 * <p>生成的 JSON 会被序列化为字符串存入 {@link RecipeGraphStateKeys#RECIPE_JSON}，
 * 供后续营养分析、效果图生成、数据库保存等节点使用。</p>
 */
@Component
@RequiredArgsConstructor
public class RecipeGenerateNode implements NodeAction {

    private final LlmRecipeService llmRecipeService;
    private final AgentStateService agentStateService;
    private final ObjectMapper objectMapper;

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> apply(OverAllState state) {
        try {
            // 汇总所有上下文信息
            List<String> ingredients = (List<String>) state.value(RecipeGraphStateKeys.INGREDIENTS).orElse(List.of());
            String cuisineName = state.value(RecipeGraphStateKeys.CUISINE_NAME, String.class).orElse("");
            String customRequire = state.value(RecipeGraphStateKeys.CUSTOM_REQUIRE, String.class).orElse("");
            String ragContext = state.value(RecipeGraphStateKeys.RAG_CONTEXT, String.class).orElse("");
            String webContext = state.value(RecipeGraphStateKeys.WEB_CONTEXT, String.class).orElse("");
            String sessionId = state.value(RecipeGraphStateKeys.SESSION_ID, String.class).orElse("");

            // 调用 DeepSeek API，返回结构化 DTO
            GeneratedRecipePayload payload = llmRecipeService.generateRecipe(
                    ingredients, cuisineName, customRequire, ragContext, webContext);
            String recipeJson = objectMapper.writeValueAsString(payload);

            agentStateService.mergeState(sessionId, Map.of(
                    RecipeGraphStateKeys.RECIPE_JSON, recipeJson,
                    RecipeGraphStateKeys.STATUS, "recipe_generated"
            ));
            return Map.of(RecipeGraphStateKeys.RECIPE_JSON, recipeJson);
        } catch (Exception e) {
            throw new IllegalStateException("菜谱生成失败", e);
        }
    }
}
