package org.carey.masterchef.config;

import com.alibaba.cloud.ai.graph.CompiledGraph;
import com.alibaba.cloud.ai.graph.OverAllState;
import com.alibaba.cloud.ai.graph.OverAllStateFactory;
import com.alibaba.cloud.ai.graph.StateGraph;
import com.alibaba.cloud.ai.graph.state.strategy.ReplaceStrategy;
import org.carey.masterchef.graph.RecipeGraphStateKeys;
import org.carey.masterchef.graph.node.ImageGenerateNode;
import org.carey.masterchef.graph.node.NutritionAnalysisNode;
import org.carey.masterchef.graph.node.RagSearchNode;
import org.carey.masterchef.graph.node.RecipeGenerateNode;
import org.carey.masterchef.graph.node.SaveRecipeNode;
import org.carey.masterchef.graph.node.WebSearchNode;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import static com.alibaba.cloud.ai.graph.StateGraph.END;
import static com.alibaba.cloud.ai.graph.StateGraph.START;
import static com.alibaba.cloud.ai.graph.action.AsyncNodeAction.node_async;

/**
 * Spring AI Alibaba Graph 工作流配置类。
 * <p>
 * 本类负责定义「菜谱生成 Agent」的完整执行链路。Graph 是一种有向图结构，
 * 每个节点（Node）完成一步 AI 或业务操作，节点之间通过边（Edge）串联。
 * </p>
 *
 * <p>整体流程如下：</p>
 * <pre>
 * START → RAG检索 → 联网搜索 → DeepSeek生成菜谱 → DeepSeek营养分析 → MiMo效果图 → 保存MySQL → END
 * </pre>
 *
 * <p><b>核心概念：</b></p>
 * <ul>
 *   <li><b>OverAllState</b> — 贯穿整个工作流的「共享状态」，类似一个 Map，各节点从中读/写数据</li>
 *   <li><b>ReplaceStrategy</b> — 状态更新策略：新值直接覆盖旧值（而非追加合并）</li>
 *   <li><b>node_async</b> — 将 NodeAction 包装为异步节点，避免阻塞 Graph 调度线程</li>
 *   <li><b>CompiledGraph</b> — 编译后的可执行图，调用 {@code invoke()} 即可运行整条链路</li>
 * </ul>
 */
@Configuration
public class GraphWorkflowConfig {

    /**
     * 定义菜谱生成状态图（StateGraph）。
     * <p>
     * Spring 会将此方法返回的 Bean 注册到容器；各 Node 类（如 RagSearchNode）
     * 通过构造器注入自动传入，无需手动 new。
     * </p>
     */
    @Bean
    public StateGraph recipeStateGraph(RagSearchNode ragSearchNode,
                                       WebSearchNode webSearchNode,
                                       RecipeGenerateNode recipeGenerateNode,
                                       NutritionAnalysisNode nutritionAnalysisNode,
                                       ImageGenerateNode imageGenerateNode,
                                       SaveRecipeNode saveRecipeNode) throws Exception {

        // 状态工厂：每次执行工作流时创建一份新的 OverAllState，并注册所有会用到的键
        OverAllStateFactory stateFactory = () -> {
            OverAllState state = new OverAllState();
            // 为每个状态键注册「替换策略」，确保节点写入时直接覆盖而非追加
            state.registerKeyAndStrategy(RecipeGraphStateKeys.SESSION_ID, new ReplaceStrategy());      // 会话 ID
            state.registerKeyAndStrategy(RecipeGraphStateKeys.INGREDIENTS, new ReplaceStrategy());     // 用户选的食材
            state.registerKeyAndStrategy(RecipeGraphStateKeys.CUISINE_CODE, new ReplaceStrategy());    // 菜系编码
            state.registerKeyAndStrategy(RecipeGraphStateKeys.CUISINE_NAME, new ReplaceStrategy());    // 菜系名称
            state.registerKeyAndStrategy(RecipeGraphStateKeys.CUSTOM_REQUIRE, new ReplaceStrategy());  // 用户自定义要求
            state.registerKeyAndStrategy(RecipeGraphStateKeys.RAG_CONTEXT, new ReplaceStrategy());     // RAG 检索到的知识
            state.registerKeyAndStrategy(RecipeGraphStateKeys.WEB_CONTEXT, new ReplaceStrategy());     // 联网搜索到的内容
            state.registerKeyAndStrategy(RecipeGraphStateKeys.RECIPE_JSON, new ReplaceStrategy());     // DeepSeek 生成的菜谱 JSON
            state.registerKeyAndStrategy(RecipeGraphStateKeys.NUTRITION_JSON, new ReplaceStrategy());  // 营养分析 JSON
            state.registerKeyAndStrategy(RecipeGraphStateKeys.IMAGE_URL, new ReplaceStrategy());       // 菜品效果图 URL
            state.registerKeyAndStrategy(RecipeGraphStateKeys.RECIPE_ID, new ReplaceStrategy());       // 保存到 MySQL 后的主键
            state.registerKeyAndStrategy(RecipeGraphStateKeys.STATUS, new ReplaceStrategy());          // 当前进度状态
            state.registerKeyAndStrategy(RecipeGraphStateKeys.ERROR, new ReplaceStrategy());           // 错误信息
            return state;
        };

        // 构建有向图：addNode 添加节点，addEdge 连接节点
        return new StateGraph("MasterChef Recipe Workflow", stateFactory)
                .addNode("rag_search", node_async(ragSearchNode))           // 第1步：RAG 向量检索
                .addNode("web_search", node_async(webSearchNode))           // 第2步：智谱联网搜索
                .addNode("recipe_generate", node_async(recipeGenerateNode)) // 第3步：DeepSeek 生成菜谱
                .addNode("nutrition_analysis", node_async(nutritionAnalysisNode)) // 第4步：营养分析
                .addNode("image_generate", node_async(imageGenerateNode))   // 第5步：MiMo 生成效果图
                .addNode("save_recipe", node_async(saveRecipeNode))         // 第6步：持久化到数据库
                // 定义执行顺序（线性链路，无分支）
                .addEdge(START, "rag_search")
                .addEdge("rag_search", "web_search")
                .addEdge("web_search", "recipe_generate")
                .addEdge("recipe_generate", "nutrition_analysis")
                .addEdge("nutrition_analysis", "image_generate")
                .addEdge("image_generate", "save_recipe")
                .addEdge("save_recipe", END);
    }

    /**
     * 将 StateGraph 编译为可执行的 CompiledGraph。
     * <p>
     * {@link org.carey.masterchef.service.RecipeGenerateRunner} 会注入此 Bean，
     * 在后台线程中调用 {@code recipeGraph.invoke(initialState)} 执行完整工作流。
     * </p>
     */
    @Bean
    public CompiledGraph recipeGraph(StateGraph recipeStateGraph) throws Exception {
        return recipeStateGraph.compile();
    }
}
