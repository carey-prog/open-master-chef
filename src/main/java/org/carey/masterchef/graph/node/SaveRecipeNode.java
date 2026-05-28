package org.carey.masterchef.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.carey.masterchef.domain.dto.GeneratedRecipePayload;
import org.carey.masterchef.graph.RecipeGraphStateKeys;
import org.carey.masterchef.service.AgentStateService;
import org.carey.masterchef.service.RecipeService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Graph 工作流第 6 步（最后一步）：持久化保存节点。
 * <p>
 * 将前面所有节点产出的数据（菜谱、营养、图片、RAG/联网上下文等）
 * 写入 MySQL 数据库，并将会话状态更新为 {@code completed}。
 * </p>
 *
 * <p>保存成功后，{@link RecipeGraphStateKeys#RECIPE_ID} 会被设置，
 * 前端可通过此 ID 调用 {@code GET /api/recipe/{id}} 获取完整菜谱详情。</p>
 */
@Component
@RequiredArgsConstructor
public class SaveRecipeNode implements NodeAction {

    private final RecipeService recipeService;
    private final AgentStateService agentStateService;
    private final ObjectMapper objectMapper;

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> apply(OverAllState state) {
        try {
            // 从共享状态中收集全部字段，准备入库
            String sessionId = state.value(RecipeGraphStateKeys.SESSION_ID, String.class).orElse("");
            List<String> ingredients = (List<String>) state.value(RecipeGraphStateKeys.INGREDIENTS).orElse(List.of());
            String cuisineCode = state.value(RecipeGraphStateKeys.CUISINE_CODE, String.class).orElse("");
            String cuisineName = state.value(RecipeGraphStateKeys.CUISINE_NAME, String.class).orElse("");
            String customRequire = state.value(RecipeGraphStateKeys.CUSTOM_REQUIRE, String.class).orElse("");
            String ragContext = state.value(RecipeGraphStateKeys.RAG_CONTEXT, String.class).orElse("");
            String webContext = state.value(RecipeGraphStateKeys.WEB_CONTEXT, String.class).orElse("");
            String recipeJson = state.value(RecipeGraphStateKeys.RECIPE_JSON, String.class).orElse("{}");
            String nutritionJson = state.value(RecipeGraphStateKeys.NUTRITION_JSON, String.class).orElse("{}");
            String imageUrl = state.value(RecipeGraphStateKeys.IMAGE_URL, String.class).orElse("");

            GeneratedRecipePayload payload = objectMapper.readValue(recipeJson, GeneratedRecipePayload.class);
            Map<String, Object> nutrition = objectMapper.readValue(nutritionJson, new TypeReference<>() {});

            // 事务性写入 recipe / recipe_ingredient / recipe_step 等表
            Long recipeId = recipeService.saveGeneratedRecipe(
                    sessionId, ingredients, cuisineCode, cuisineName, customRequire,
                    ragContext, webContext, payload, nutrition, imageUrl);

            agentStateService.mergeState(sessionId, Map.of(
                    RecipeGraphStateKeys.RECIPE_ID, recipeId,
                    RecipeGraphStateKeys.STATUS, "completed"
            ));
            return Map.of(RecipeGraphStateKeys.RECIPE_ID, recipeId, RecipeGraphStateKeys.STATUS, "completed");
        } catch (Exception e) {
            throw new IllegalStateException("保存菜谱失败", e);
        }
    }
}
