package org.carey.masterchef.graph;

/**
 * Graph 工作流「共享状态」中所有键名的常量定义。
 * <p>
 * 工作流各节点通过 {@code OverAllState} 传递数据，键名统一在此维护，
 * 避免魔法字符串（Magic String）散落在各处，便于重构和阅读。
 * </p>
 *
 * <p>数据在各节点间的流转示意：</p>
 * <pre>
 * 用户请求 → SESSION_ID / INGREDIENTS / CUISINE_* / CUSTOM_REQUIRE
 *     ↓
 * RAG 节点 → RAG_CONTEXT
 *     ↓
 * 联网搜索 → WEB_CONTEXT
 *     ↓
 * 菜谱生成 → RECIPE_JSON
 *     ↓
 * 营养分析 → NUTRITION_JSON
 *     ↓
 * 效果图   → IMAGE_URL
 *     ↓
 * 保存     → RECIPE_ID + STATUS=completed
 * </pre>
 */
public final class RecipeGraphStateKeys {

    /** 私有构造器，禁止实例化工具类 */
    private RecipeGraphStateKeys() {
    }

    /** 会话唯一标识，前端轮询进度时使用，同时作为 Redis 存储键的一部分 */
    public static final String SESSION_ID = "sessionId";

    /** 用户选择的食材列表，如 ["猪肉", "白菜"] */
    public static final String INGREDIENTS = "ingredients";

    /** 菜系编码，对应数据库 cuisine 表的 code 字段，如 "sichuan" */
    public static final String CUISINE_CODE = "cuisineCode";

    /** 菜系中文名，如 "川菜"，用于拼接待检索/生成的 Prompt */
    public static final String CUISINE_NAME = "cuisineName";

    /** 用户额外要求，如 "少油少盐"、"适合儿童" */
    public static final String CUSTOM_REQUIRE = "customRequire";

    /** RAG 节点检索到的烹饪知识上下文（来自 Redis Stack 向量库） */
    public static final String RAG_CONTEXT = "ragContext";

    /** 联网搜索节点返回的最新做法/流行菜谱参考（来自智谱 WebSearch） */
    public static final String WEB_CONTEXT = "webContext";

    /** DeepSeek 生成的完整菜谱 JSON 字符串 */
    public static final String RECIPE_JSON = "recipeJson";

    /** DeepSeek 营养分析结果 JSON 字符串 */
    public static final String NUTRITION_JSON = "nutritionJson";

    /** 菜品效果图 URL（当前多为占位图路径） */
    public static final String IMAGE_URL = "imageUrl";

    /** 菜谱保存到 MySQL 后生成的主键 ID */
    public static final String RECIPE_ID = "recipeId";

    /**
     * 工作流进度状态，前端据此展示进度条。
     * 取值示例：running → rag_done → web_search_done → recipe_generated → nutrition_done → image_done → completed / failed
     */
    public static final String STATUS = "status";

    /** 失败时的错误描述，供前端展示 */
    public static final String ERROR = "error";
}
