package org.carey.masterchef.service;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.carey.masterchef.graph.RecipeGraphStateKeys;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.Map;
import java.util.Optional;

/**
 * Graph 工作流的异步执行器。
 * <p>
 * 为什么需要单独的 Runner？因为 Graph 完整执行可能需要 30~120 秒（多次 AI API 调用），
 * 若在 Controller 线程中同步执行会导致 HTTP 超时。因此使用 {@code @Async} 在独立线程池中运行。
 * </p>
 *
 * <p>线程池配置见 {@link org.carey.masterchef.config.AsyncConfig}，Bean 名称为 {@code recipeExecutor}。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class RecipeGenerateRunner {

    /** 编译后的 Graph，由 GraphWorkflowConfig 注入 */
    private final CompiledGraph recipeGraph;
    private final AgentStateService agentStateService;

    /**
     * 在后台线程中执行完整 Graph 工作流。
     *
     * @param sessionId    会话 ID，用于更新 Redis 状态
     * @param initialState   Graph 初始状态（用户输入 + 会话信息）
     */
    @Async("recipeExecutor")
    public void run(String sessionId, Map<String, Object> initialState) {
        try {
            log.info("开始异步生成菜谱 sessionId={}", sessionId);

            // invoke 会按 GraphWorkflowConfig 定义的边顺序依次执行 6 个节点
            Optional<OverAllState> result = recipeGraph.invoke(initialState);
            OverAllState finalState = result.orElseThrow(() -> new IllegalStateException("工作流未返回结果"));

            // 将最终状态完整写入 Redis
            agentStateService.saveState(sessionId, finalState.data());

            // 校验是否成功拿到 recipeId（SaveRecipeNode 写入）
            Long recipeId = finalState.value(RecipeGraphStateKeys.RECIPE_ID, Long.class).orElse(null);
            if (recipeId == null) {
                String error = finalState.value(RecipeGraphStateKeys.ERROR, String.class).orElse("生成失败");
                throw new IllegalStateException(error);
            }
            log.info("菜谱生成完成 sessionId={}, recipeId={}", sessionId, recipeId);
        } catch (Exception e) {
            log.error("菜谱生成工作流失败 sessionId={}", sessionId, e);
            // 失败时更新 Redis 状态，前端轮询到 status=failed 后停止并展示错误
            agentStateService.mergeState(sessionId, Map.of(
                    RecipeGraphStateKeys.STATUS, "failed",
                    RecipeGraphStateKeys.ERROR, e.getMessage() != null ? e.getMessage() : "未知错误"
            ));
        }
    }
}
