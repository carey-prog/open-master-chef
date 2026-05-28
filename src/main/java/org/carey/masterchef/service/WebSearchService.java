package org.carey.masterchef.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/**
 * 联网搜索门面服务，统一对外提供 search 方法。
 * <p>
 * 内部优先使用 {@link ZhipuWebSearchService}（智谱 WebSearch API）；
 * 若智谱未配置或调用失败，自动降级为本地提示文本，保证 Graph 工作流不中断。
 * </p>
 *
 * <p>使用 {@link ObjectProvider} 而非直接注入 ZhipuWebSearchService，
 * 是因为后者带有 {@code @Conditional} 条件注解，Key 缺失时 Bean 不存在，
 * ObjectProvider 可以安全地 {@code getIfAvailable()} 而不抛异常。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class WebSearchService {

    private final ObjectProvider<ZhipuWebSearchService> zhipuWebSearchService;

    /**
     * 执行联网搜索。
     *
     * @param query 搜索关键词
     * @return 搜索结果 JSON 字符串，或降级提示文本
     */
    public String search(String query) {
        ZhipuWebSearchService zhipu = zhipuWebSearchService.getIfAvailable();
        if (zhipu == null) {
            log.warn("智谱 WebSearch 未启用，使用降级提示。请在 application.yml 配置 zhipu.api-key（格式 id.secret）");
            return buildFallbackContext(query);
        }
        try {
            return zhipu.search(query);
        } catch (Exception e) {
            log.warn("智谱联网搜索失败: {}", e.getMessage());
            return buildFallbackContext(query);
        }
    }

    /** 智谱不可用时的降级文本，仍会被注入 DeepSeek Prompt 作为参考 */
    private String buildFallbackContext(String query) {
        return "当前未接入联网搜索，请结合常见烹饪经验与流行趋势进行创作。搜索主题：" + query;
    }
}
