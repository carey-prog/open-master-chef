package org.carey.masterchef.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.carey.masterchef.config.AgentProperties;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

/**
 * Agent 会话状态管理服务（基于 Redis）。
 * <p>
 * Graph 工作流执行过程中，各节点会频繁更新进度（如 rag_done、recipe_generated 等），
 * 前端通过轮询 API 读取这些状态。Redis 的高读写性能非常适合这种场景。
 * </p>
 *
 * <p>Redis Key 格式：{@code agent:session:{sessionId}}，TTL 默认 24 小时（见 application.yml）。</p>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AgentStateService {

    private final RedisTemplate<String, Object> redisTemplate;
    private final AgentProperties agentProperties;
    private final ObjectMapper objectMapper;

    /** 生成无横线的 UUID 作为 sessionId */
    public String createSessionId() {
        return UUID.randomUUID().toString().replace("-", "");
    }

    /**
     * 全量保存会话状态（覆盖 Redis 中的旧值）。
     *
     * @param sessionId 会话 ID
     * @param state     完整状态 Map
     */
    public void saveState(String sessionId, Map<String, Object> state) {
        String key = agentProperties.getSessionPrefix() + sessionId;
        redisTemplate.opsForValue().set(key, state, Duration.ofHours(agentProperties.getStateTtlHours()));
    }

    /**
     * 读取会话状态。
     *
     * @return Optional 包装的状态 Map，不存在或已过期时返回 empty
     */
    @SuppressWarnings("unchecked")
    public Optional<Map<String, Object>> getState(String sessionId) {
        String key = agentProperties.getSessionPrefix() + sessionId;
        Object value = redisTemplate.opsForValue().get(key);
        if (value == null) {
            return Optional.empty();
        }
        if (value instanceof Map<?, ?> map) {
            // Redis 反序列化后可能是 LinkedHashMap，需转为标准 Map<String, Object>
            return Optional.of(objectMapper.convertValue(map, new TypeReference<Map<String, Object>>() {}));
        }
        return Optional.empty();
    }

    /**
     * 增量合并状态：读取现有状态，用 partial 中的键值覆盖/追加，再写回 Redis。
     * Graph 各节点调用此方法更新进度，不会丢失其他字段。
     */
    public void mergeState(String sessionId, Map<String, Object> partial) {
        Map<String, Object> current = getState(sessionId).orElseGet(HashMap::new);
        current.putAll(partial);
        saveState(sessionId, current);
    }

    /** 快捷方法：仅更新 status 字段 */
    public void updateStatus(String sessionId, String status) {
        mergeState(sessionId, Map.of("status", status));
    }
}
