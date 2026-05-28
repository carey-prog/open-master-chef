package org.carey.masterchef.service;

import ai.z.openapi.ZhipuAiClient;
import ai.z.openapi.service.web_search.WebSearchRequest;
import ai.z.openapi.service.web_search.WebSearchResponse;
import ai.z.openapi.service.web_search.WebSearchService;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.carey.masterchef.config.ZhipuApiKeyPresentCondition;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Service;

/**
 * 智谱 AI 联网搜索（WebSearch）具体实现。
 * <p>
 * 调用智谱 {@code search_pro} 搜索引擎，获取互联网上的最新菜谱、做法等信息。
 * API Key 格式为 {@code id.secret}，在 application.yml 的 {@code zhipu.api-key} 中配置。
 * </p>
 *
 * <p>仅当有效 API Key 存在时才注册为 Spring Bean（{@link ZhipuApiKeyPresentCondition}）。</p>
 */
@Slf4j
@Service
@Conditional(ZhipuApiKeyPresentCondition.class)
public class ZhipuWebSearchService {

    private static final int MAX_RESULTS = 5;
    private static final int MAX_ITEM_CONTENT_CHARS = 600;
    private static final int MAX_TOTAL_CHARS = 12000;

    private final WebSearchService webSearchService;
    private final ObjectMapper objectMapper;

    public ZhipuWebSearchService(ZhipuAiClient zhipuAiClient, ObjectMapper objectMapper) {
        // 从 ZhipuAiClient 获取 WebSearch 子服务
        this.webSearchService = zhipuAiClient.webSearch();
        this.objectMapper = objectMapper;
    }

    /**
     * 调用智谱 WebSearch API 并返回 JSON 格式的搜索结果。
     *
     * @param searchQuery 搜索关键词
     * @return 搜索结果的 JSON 字符串（含标题、摘要、链接等）
     */
    public String search(String searchQuery) {
        WebSearchRequest request = WebSearchRequest.builder()
                .searchEngine("search_pro")
                .searchQuery(searchQuery)
                .count(MAX_RESULTS)
                .searchRecencyFilter("noLimit")
                .contentSize("medium")
                .build();
        log.info("调用智谱 WebSearch，query={}", searchQuery);

        WebSearchResponse response = webSearchService.createWebSearch(request);
        return compactSearchResult(response);
    }

    /**
     * 将 WebSearch 完整响应压缩为精简文本，避免入库/注入 Prompt 时超出字段或 token 限制。
     */
    private String compactSearchResult(WebSearchResponse response) {
        try {
            JsonNode root = objectMapper.valueToTree(response);
            JsonNode results = findResultsArray(root);
            if (results == null || !results.isArray() || results.isEmpty()) {
                String raw = objectMapper.writeValueAsString(response);
                return truncate(raw, MAX_TOTAL_CHARS);
            }

            StringBuilder sb = new StringBuilder();
            int index = 0;
            for (JsonNode item : results) {
                if (index >= MAX_RESULTS || sb.length() >= MAX_TOTAL_CHARS) {
                    break;
                }
                String title = firstText(item, "title", "name");
                String content = truncate(firstText(item, "content", "snippet", "summary"), MAX_ITEM_CONTENT_CHARS);
                if (!title.isBlank() || !content.isBlank()) {
                    sb.append(index + 1).append(". ").append(title).append('\n');
                    if (!content.isBlank()) {
                        sb.append(content).append('\n');
                    }
                    sb.append('\n');
                    index++;
                }
            }
            String compact = sb.toString().trim();
            return compact.isBlank() ? truncate(objectMapper.writeValueAsString(response), MAX_TOTAL_CHARS) : compact;
        } catch (JsonProcessingException e) {
            log.warn("WebSearch 结果压缩失败，回退为 toString", e);
            return truncate(String.valueOf(response), MAX_TOTAL_CHARS);
        }
    }

    private JsonNode findResultsArray(JsonNode root) {
        for (String key : new String[]{"search_result", "searchResult", "results", "data"}) {
            JsonNode node = root.path(key);
            if (node.isArray()) {
                return node;
            }
        }
        return null;
    }

    private String firstText(JsonNode node, String... fields) {
        for (String field : fields) {
            JsonNode value = node.path(field);
            if (value.isTextual() && !value.asText().isBlank()) {
                return value.asText().trim();
            }
        }
        return "";
    }

    private String truncate(String text, int maxChars) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxChars) {
            return text;
        }
        return text.substring(0, maxChars) + "\n...(truncated)";
    }
}
