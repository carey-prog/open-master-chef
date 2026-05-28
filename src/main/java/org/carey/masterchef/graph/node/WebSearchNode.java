package org.carey.masterchef.graph.node;

import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.action.NodeAction;
import lombok.RequiredArgsConstructor;
import org.carey.masterchef.graph.RecipeGraphStateKeys;
import org.carey.masterchef.service.AgentStateService;
import org.carey.masterchef.service.WebSearchService;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * Graph 工作流第 2 步：联网搜索节点。
 * <p>
 * 与 RAG 检索本地知识库不同，本节点通过智谱 AI WebSearch API
 * 获取互联网上的最新做法、流行菜谱等实时信息，补充大模型的知识盲区。
 * </p>
 *
 * <p>若智谱 API Key 未配置或调用失败，{@link WebSearchService} 会返回降级提示文本，
 * 工作流不会中断，DeepSeek 仍可基于 RAG 上下文生成菜谱。</p>
 */
@Component
@RequiredArgsConstructor
public class WebSearchNode implements NodeAction {

    private final WebSearchService webSearchService;
    private final AgentStateService agentStateService;

    @Override
    @SuppressWarnings("unchecked")
    public Map<String, Object> apply(OverAllState state) {
        List<String> ingredients = (List<String>) state.value(RecipeGraphStateKeys.INGREDIENTS).orElse(List.of());
        String cuisineName = state.value(RecipeGraphStateKeys.CUISINE_NAME, String.class).orElse("");

        // 搜索关键词侧重「最新」和「流行」，与 RAG 节点的「技巧/搭配」形成互补
        String query = cuisineName + " " + String.join(" ", ingredients) + " 最新做法 流行菜谱";
        String webContext = webSearchService.search(query);

        String sessionId = state.value(RecipeGraphStateKeys.SESSION_ID, String.class).orElse("");
        agentStateService.mergeState(sessionId, Map.of(
                RecipeGraphStateKeys.WEB_CONTEXT, webContext,
                RecipeGraphStateKeys.STATUS, "web_search_done"
        ));
        return Map.of(RecipeGraphStateKeys.WEB_CONTEXT, webContext);
    }
}
