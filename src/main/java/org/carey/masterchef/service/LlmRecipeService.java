package org.carey.masterchef.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.carey.masterchef.config.DeepSeekProperties;
import org.carey.masterchef.domain.dto.GeneratedRecipePayload;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 基于 DeepSeek 大模型的菜谱生成与营养分析服务。
 * <p>
 * 本类封装了与 DeepSeek API 的所有交互：构造 Prompt、发送请求、解析 JSON 响应。
 * Graph 工作流中的 {@code RecipeGenerateNode} 和 {@code NutritionAnalysisNode} 都依赖此服务。
 * </p>
 *
 * <p><b>Prompt 工程要点：</b></p>
 * <ul>
 *   <li>明确要求返回纯 JSON，不要 markdown 代码块</li>
 *   <li>给出完整的 JSON 格式示例，减少大模型输出格式错误</li>
 *   <li>注入 RAG 和联网搜索上下文，提升生成质量</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class LlmRecipeService {

    /** Spring AI 高层客户端，底层使用 AiConfig 配置的 DeepSeek ChatModel */
    private final ChatClient deepSeekChatClient;
    private final DeepSeekProperties deepSeekProperties;
    private final ObjectMapper objectMapper;

    /**
     * 调用 DeepSeek 生成完整菜谱。
     *
     * @param ingredients   用户现有食材
     * @param cuisineName   菜系名称（如「川菜」）
     * @param customRequire 用户自定义要求
     * @param ragContext    RAG 检索到的专业知识
     * @param webContext    联网搜索到的流行趋势
     * @return 结构化菜谱 DTO
     */
    public GeneratedRecipePayload generateRecipe(List<String> ingredients,
                                                 String cuisineName,
                                                 String customRequire,
                                                 String ragContext,
                                                 String webContext) {
        // 使用 Java 文本块（Text Block）编写多行 Prompt，可读性更好
        String prompt = """
                你是一位%s大师厨师。请根据以下信息生成一份完整菜谱。
                
                【用户现有食材】%s
                【自定义要求】%s
                【专业知识参考】%s
                【流行趋势参考】%s
                
                请严格返回 JSON，不要 markdown 代码块，格式如下：
                {
                  "title": "菜名",
                  "difficulty": "简单/中等/困难",
                  "cookingTime": 45,
                  "summary": "一句话简介",
                  "ingredients": [
                    {"category":"主料","name":"猪肉","amount":"300g"},
                    {"category":"配菜","name":"白菜","amount":"100g"},
                    {"category":"调料","name":"盐","amount":"1茶匙"}
                  ],
                  "steps": ["步骤1...", "步骤2..."]
                }
                """.formatted(
                cuisineName,
                String.join("、", ingredients),
                customRequire != null && !customRequire.isBlank() ? customRequire : "无",
                ragContext != null && !ragContext.isBlank() ? ragContext : "无",
                webContext != null && !webContext.isBlank() ? webContext : "无"
        );

        log.info("调用 DeepSeek 生成菜谱，model={}，RAG上下文={} 字符，联网上下文={} 字符",
                deepSeekProperties.getModel(),
                ragContext != null ? ragContext.length() : 0,
                webContext != null ? webContext.length() : 0);

        // ChatClient 链式调用：prompt → user消息 → call → 取 content 文本
        String response = deepSeekChatClient.prompt()
                .user(prompt)
                .call()
                .content();

        return parseRecipeJson(response);
    }

    /**
     * 调用 DeepSeek 对已有菜谱做营养分析。
     *
     * @param recipe 上一步生成的菜谱对象
     * @return 营养信息 Map（热量、蛋白质、维生素等）
     */
    public Map<String, Object> analyzeNutrition(GeneratedRecipePayload recipe) {
        String prompt = """
                请对以下菜谱进行营养分析，返回 JSON（不要 markdown）：
                菜名：%s
                食材：%s
                
                格式：
                {
                  "calories": "约450kcal/份",
                  "protein": "25g",
                  "fat": "18g",
                  "carbs": "35g",
                  "fiber": "5g",
                  "vitamins": ["维生素A", "维生素C"],
                  "minerals": ["钙", "铁"],
                  "healthTips": ["适合...", "注意..."],
                  "suitableFor": ["普通人群"],
                  "notSuitableFor": ["低钠饮食者"]
                }
                """.formatted(
                recipe.getTitle(),
                recipe.getIngredients().stream()
                        .map(i -> i.getName() + i.getAmount())
                        .reduce((a, b) -> a + "、" + b)
                        .orElse("")
        );

        log.info("调用 DeepSeek 营养分析，model={}，菜名={}", deepSeekProperties.getModel(), recipe.getTitle());

        String response = deepSeekChatClient.prompt()
                .user(prompt)
                .call()
                .content();

        return parseNutritionJson(response);
    }

    /** 解析菜谱 JSON，失败时抛异常（菜谱格式错误属于致命错误，需中断工作流） */
    private GeneratedRecipePayload parseRecipeJson(String response) {
        try {
            String json = extractJson(response);
            return objectMapper.readValue(json, GeneratedRecipePayload.class);
        } catch (Exception e) {
            log.error("解析菜谱 JSON 失败: {}", response, e);
            throw new IllegalStateException("大模型返回格式异常，请重试");
        }
    }

    /** 解析营养 JSON，失败时返回默认值（营养分析非致命，不应阻断工作流） */
    @SuppressWarnings("unchecked")
    private Map<String, Object> parseNutritionJson(String response) {
        try {
            String json = extractJson(response);
            return objectMapper.readValue(json, Map.class);
        } catch (Exception e) {
            log.warn("解析营养 JSON 失败，返回默认值");
            return Map.of(
                    "calories", "约400kcal/份",
                    "healthTips", List.of("请根据个人体质适量食用"),
                    "suitableFor", List.of("普通人群")
            );
        }
    }

    /**
     * 从大模型回复中提取 JSON 字符串。
     * 大模型有时会用 ```json ... ``` 包裹，此方法会剥离 markdown 标记，只保留 { ... } 部分。
     */
    private String extractJson(String text) {
        String trimmed = text.trim();
        if (trimmed.startsWith("```")) {
            int start = trimmed.indexOf('{');
            int end = trimmed.lastIndexOf('}');
            if (start >= 0 && end > start) {
                return trimmed.substring(start, end + 1);
            }
        }
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        return trimmed;
    }
}
