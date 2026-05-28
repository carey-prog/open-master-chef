package org.carey.masterchef.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.carey.masterchef.domain.dto.GeneratedRecipePayload;
import org.carey.masterchef.graph.RecipeGraphStateKeys;
import org.carey.masterchef.service.AgentStateService;
import org.carey.masterchef.service.DishImageService;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * Graph 工作流第 5 步：菜品效果图生成节点。
 * <p>使用 DashScope 通义万相文生图，失败时回退占位 SVG。</p>
 */
@Component
@RequiredArgsConstructor
public class ImageGenerateNode implements NodeAction {

    private final DishImageService dishImageService;
    private final AgentStateService agentStateService;
    private final ObjectMapper objectMapper;

    @Override
    public Map<String, Object> apply(OverAllState state) {
        try {
            String recipeJson = state.value(RecipeGraphStateKeys.RECIPE_JSON, String.class).orElse("{}");
            GeneratedRecipePayload payload = objectMapper.readValue(recipeJson, GeneratedRecipePayload.class);

            String imageUrl = dishImageService.generateDishImage(payload.getTitle(), payload.getSummary());
            String sessionId = state.value(RecipeGraphStateKeys.SESSION_ID, String.class).orElse("");

            agentStateService.mergeState(sessionId, Map.of(
                    RecipeGraphStateKeys.IMAGE_URL, imageUrl,
                    RecipeGraphStateKeys.STATUS, "image_done"
            ));
            return Map.of(RecipeGraphStateKeys.IMAGE_URL, imageUrl);
        } catch (Exception e) {
            throw new IllegalStateException("效果图生成失败", e);
        }
    }
}
