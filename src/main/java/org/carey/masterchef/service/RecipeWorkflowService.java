package org.carey.masterchef.service;

import lombok.RequiredArgsConstructor;
import org.carey.masterchef.domain.dto.RecipeGenerateRequest;
import org.carey.masterchef.domain.entity.Cuisine;
import org.carey.masterchef.graph.RecipeGraphStateKeys;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

/**
 * 菜谱生成工作流的「门面服务」，对外提供启动生成和查询会话状态的接口。
 * <p>
 * 设计为<b>异步模式</b>：{@link #startGenerate} 立即返回 sessionId，
 * 实际 Graph 执行在后台线程中进行（见 {@link RecipeGenerateRunner}），
 * 前端通过轮询 {@link #getSessionState} 获取进度，避免 HTTP 请求长时间阻塞。
 * </p>
 */
@Service
@RequiredArgsConstructor
public class RecipeWorkflowService {

    /** 管理 Redis 中的 Agent 会话状态（进度、结果等） */
    private final AgentStateService agentStateService;
    /** 查询菜系、食材等基础数据 */
    private final IngredientService ingredientService;
    /** 在后台线程中执行 Graph 工作流 */
    private final RecipeGenerateRunner recipeGenerateRunner;

    /**
     * 启动一次菜谱生成任务。
     *
     * @param request 前端提交的生成请求（食材、菜系、自定义要求等）
     * @return sessionId 会话 ID，前端用于轮询进度
     */
    public String startGenerate(RecipeGenerateRequest request) {
        // 若前端未传 sessionId，则自动生成一个 UUID（去掉横线）
        String sessionId = request.getSessionId() != null && !request.getSessionId().isBlank()
                ? request.getSessionId()
                : agentStateService.createSessionId();

        // 根据菜系编码查中文名，用于 Prompt 和展示
        Cuisine cuisine = ingredientService.getCuisineByCode(request.getCuisineCode());
        String cuisineName = cuisine != null ? cuisine.getName() : request.getCuisineCode();

        // 构建 Graph 工作流的初始状态（Map 会被传入 CompiledGraph.invoke）
        Map<String, Object> initialState = new HashMap<>();
        initialState.put(RecipeGraphStateKeys.SESSION_ID, sessionId);
        initialState.put(RecipeGraphStateKeys.INGREDIENTS, request.getIngredients());
        initialState.put(RecipeGraphStateKeys.CUISINE_CODE, request.getCuisineCode());
        initialState.put(RecipeGraphStateKeys.CUISINE_NAME, cuisineName);
        initialState.put(RecipeGraphStateKeys.CUSTOM_REQUIRE, request.getCustomRequire());
        initialState.put(RecipeGraphStateKeys.STATUS, "running");

        // 先写入 Redis，再异步启动 Graph（保证前端轮询时能立刻看到 running 状态）
        agentStateService.saveState(sessionId, initialState);
        recipeGenerateRunner.run(sessionId, initialState);
        return sessionId;
    }

    /**
     * 查询指定会话的当前状态，供前端轮询。
     * 返回 Map 中包含 status、recipeId、error 等字段。
     */
    public Map<String, Object> getSessionState(String sessionId) {
        return agentStateService.getState(sessionId).orElse(Map.of());
    }
}
