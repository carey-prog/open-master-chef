package org.carey.masterchef.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.TextReader;
import org.springframework.ai.transformer.splitter.TextSplitter;
import org.springframework.ai.transformer.splitter.TokenTextSplitter;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * RAG（Retrieval-Augmented Generation）检索服务。
 * <p>
 * 职责分两部分：</p>
 * <ol>
 *   <li><b>启动时建库</b> — 读取 {@code classpath:rag/cooking-knowledge.md}，切分为小块后写入 Redis 向量库</li>
 *   <li><b>运行时检索</b> — 根据 query 做语义相似度搜索，返回最相关的知识片段供大模型参考</li>
 * </ol>
 *
 * <p>若 VectorStore 不可用（DashScope Key 缺失或 Redis Stack 未启动），
 * 服务会优雅降级：检索返回空字符串，不影响工作流继续执行。</p>
 */
@Slf4j
@Service
public class RagService {

    /** 延迟获取 VectorStore，避免 Bean 不存在时启动失败 */
    private final ObjectProvider<VectorStore> vectorStoreProvider;

    /** 烹饪知识库 markdown 文件，位于 src/main/resources/rag/ 目录 */
    @Value("classpath:rag/cooking-knowledge.md")
    private Resource cookingKnowledge;

    private VectorStore vectorStore;
    /** 标记知识库是否已成功加载到向量库 */
    private boolean knowledgeLoaded;

    public RagService(ObjectProvider<VectorStore> vectorStoreProvider) {
        this.vectorStoreProvider = vectorStoreProvider;
    }

    /**
     * 应用启动完成后自动执行，将知识库文档向量化并存入 Redis Stack。
     * {@code @Order(10)} 确保在 EmbeddingModel 初始化之后执行。
     */
    @EventListener(ApplicationReadyEvent.class)
    @Order(10)
    public void initKnowledgeBase() {
        this.vectorStore = vectorStoreProvider.getIfAvailable();
        if (vectorStore == null) {
            log.warn("VectorStore 未就绪，RAG 已禁用。请确认 DashScope Key 与 Redis Stack(6376/6379) 已正确配置");
            return;
        }
        try {
            // TextReader 读取 markdown 全文为一个 Document
            TextReader reader = new TextReader(cookingKnowledge);
            reader.getCustomMetadata().put("source", "cooking-knowledge");
            List<Document> documents = reader.get();

            // TokenTextSplitter 按 token 数切分长文档，避免单条向量过大
            // 参数：chunkSize=500, chunkOverlap=100（相邻块有 100 token 重叠，保证上下文连贯）
            TextSplitter splitter = new TokenTextSplitter(500, 100, 5, 10000, true);
            List<Document> chunks = splitter.apply(documents);

            // 批量写入向量库（内部会调用 EmbeddingModel 向量化）
            vectorStore.add(chunks);
            knowledgeLoaded = true;
            log.info("RAG 知识库已加载 {} 个文档片段到 Redis Stack", chunks.size());
        } catch (Exception e) {
            log.error("RAG 知识库加载失败: {}", e.getMessage(), e);
        }
    }

    /** VectorStore Bean 是否存在（不代表知识库已加载） */
    public boolean isReady() {
        return vectorStore != null;
    }

    /** 知识库文档是否已成功写入向量库 */
    public boolean isKnowledgeLoaded() {
        return knowledgeLoaded;
    }

    /**
     * 语义相似度检索。
     *
     * @param query 检索关键词/句子（会被 Embedding 后做向量搜索）
     * @param topK  返回最相似的前 K 条文档片段
     * @return 拼接后的上下文文本，多条结果用 {@code ---} 分隔；无结果或失败时返回空字符串
     */
    public String search(String query, int topK) {
        if (vectorStore == null) {
            log.warn("RAG 检索跳过：VectorStore 不可用");
            return "";
        }
        try {
            List<Document> results = vectorStore.similaritySearch(
                    SearchRequest.builder().query(query).topK(topK).build());
            if (results.isEmpty()) {
                log.info("RAG 检索无结果，query={}", query);
                return "";
            }
            String context = results.stream()
                    .map(Document::getText)
                    .collect(Collectors.joining("\n---\n"));
            log.info("RAG 检索成功，query={}，命中 {} 条，上下文长度 {} 字符", query, results.size(), context.length());
            return context;
        } catch (Exception e) {
            log.error("RAG 检索失败: {}", e.getMessage(), e);
            return "";
        }
    }
}
