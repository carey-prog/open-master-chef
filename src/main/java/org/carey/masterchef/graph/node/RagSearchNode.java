package org.carey.masterchef.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.carey.masterchef.graph.RecipeGraphStateKeys;
import org.carey.masterchef.service.AgentStateService;
import org.carey.masterchef.service.RagService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Graph 工作流第 1 步：RAG（检索增强生成）知识检索节点。
 * <p>
 * RAG 的核心思想：在调用大模型之前，先从本地知识库中检索与问题相关的文档片段，
 * 作为「专业知识参考」注入 Prompt，让生成的菜谱更专业、更准确。
 * </p>
 *
 * <p>本节点使用 DashScope text-embedding-v3 将查询向量化，
 * 再通过 Redis Stack（RediSearch）做相似度检索，知识来源为 {@code rag/cooking-knowledge.md}。</p>
 *
 * <p>实现 {@link NodeAction} 接口后，{@code apply()} 方法会在 Graph 调度到此节点时被调用，
 * 返回值会合并进 OverAllState，供后续节点读取。</p>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RagSearchNode implements NodeAction {

    private final RagService ragService;
    /** 同步更新 Redis 中的会话状态，供前端轮询展示进度 */
    private final AgentStateService agentStateService;

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> apply(OverAllState state) {
        // 从共享状态中读取上游传入的数据
        List<String> ingredients = (List<String>) state.value(RecipeGraphStateKeys.INGREDIENTS).orElse(List.of());
        String cuisineName = state.value(RecipeGraphStateKeys.CUISINE_NAME, String.class).orElse("");

        // 拼接检索 query：菜系 + 食材 + 关键词，提高向量检索命中率
        String query = cuisineName + " " + String.join(" ", ingredients) + " 烹饪技巧 搭配";
        log.info("RAG 节点开始检索，query={}", query);

        // 从向量库取 topK=5 最相关的文档片段
        String ragContext = ragService.search(query, 5);

        // 将会话进度写入 Redis，前端可通过 /api/agent/session/{sessionId} 轮询
        String sessionId = state.value(RecipeGraphStateKeys.SESSION_ID, String.class).orElse("");
        agentStateService.mergeState(sessionId, Map.of(
                RecipeGraphStateKeys.RAG_CONTEXT, ragContext,
                RecipeGraphStateKeys.STATUS, "rag_done"
        ));

        // 返回的 Map 会写入 Graph 共享状态，下一节点 WebSearchNode 可继续读取
        return Map.of(RecipeGraphStateKeys.RAG_CONTEXT, ragContext);
    }
}
