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

import java.util.Map;

/**
 * Graph 工作流第 4 步：DeepSeek 营养分析节点。
 * <p>
 * 读取上一步生成的菜谱 JSON，再次调用 DeepSeek 分析热量、蛋白质、
 * 维生素矿物质、适用人群等营养信息，结果以 JSON 形式存入共享状态。
 * </p>
 *
 * <p>营养分析失败时 {@link LlmRecipeService} 会返回默认值，不会阻断整个工作流。</p>
 */
@Component
@RequiredArgsConstructor
public class NutritionAnalysisNode implements NodeAction {

    private final LlmRecipeService llmRecipeService;
    private final AgentStateService agentStateService;
    private final ObjectMapper objectMapper;

    @Override
    public Map<String, Object> apply(OverAllState state) {
        try {
            // 从上一步的状态中反序列化菜谱对象
            String recipeJson = state.value(RecipeGraphStateKeys.RECIPE_JSON, String.class).orElse("{}");
            GeneratedRecipePayload payload = objectMapper.readValue(recipeJson, GeneratedRecipePayload.class);

            Map<String, Object> nutrition = llmRecipeService.analyzeNutrition(payload);
            String nutritionJson = objectMapper.writeValueAsString(nutrition);
            String sessionId = state.value(RecipeGraphStateKeys.SESSION_ID, String.class).orElse("");

            agentStateService.mergeState(sessionId, Map.of(
                    RecipeGraphStateKeys.NUTRITION_JSON, nutritionJson,
                    RecipeGraphStateKeys.STATUS, "nutrition_done"
            ));
            return Map.of(RecipeGraphStateKeys.NUTRITION_JSON, nutritionJson);
        } catch (Exception e) {
            throw new IllegalStateException("营养分析失败", e);
        }
    }
}
