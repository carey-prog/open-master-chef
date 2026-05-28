package org.carey.masterchef.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.carey.masterchef.domain.dto.ApiResponse;
import org.carey.masterchef.domain.dto.RecipeDetailDto;
import org.carey.masterchef.domain.dto.RecipeGenerateRequest;
import org.carey.masterchef.domain.entity.Cuisine;
import org.carey.masterchef.service.IngredientService;
import org.carey.masterchef.service.MimoVisionService;
import org.carey.masterchef.service.RecipeService;
import org.carey.masterchef.service.RecipeWorkflowService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

/**
 * 菜谱相关 REST API 控制器。
 * <p>
 * {@code @RestController} = {@code @Controller} + {@code @ResponseBody}，
 * 所有方法的返回值会自动序列化为 JSON，无需手动写 ResponseEntity。
 * </p>
 *
 * <p>前端 {@code app.js} 主要调用本类的接口完成四步交互：选食材 → 选菜系 → 生成 → 查看结果。</p>
 */
@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RecipeApiController {

    private final RecipeWorkflowService recipeWorkflowService;
    private final RecipeService recipeService;
    private final IngredientService ingredientService;
    private final MimoVisionService mimoVisionService;

    /**
     * 启动菜谱生成（异步）。
     * POST /api/recipe/generate
     * 请求体：{ ingredients, cuisineCode, customRequire, sessionId? }
     * 响应：{ sessionId, status: "running" } — 前端拿到 sessionId 后开始轮询
     */
    @PostMapping("/recipe/generate")
    public ApiResponse<Map<String, Object>> generate(@Valid @RequestBody RecipeGenerateRequest request) {
        String sessionId = recipeWorkflowService.startGenerate(request);
        return ApiResponse.ok("已开始生成", Map.of(
                "sessionId", sessionId,
                "status", "running"
        ));
    }

    /** 根据 ID 获取已保存的完整菜谱详情（含步骤、食材、营养等） */
    @GetMapping("/recipe/{id}")
    public ApiResponse<RecipeDetailDto> getRecipe(@PathVariable Long id) {
        RecipeDetailDto detail = recipeService.getRecipeDetail(id);
        if (detail == null) {
            return ApiResponse.fail("菜谱不存在");
        }
        return ApiResponse.ok(detail);
    }

    /** 获取食材分类及下属食材列表，供首页展示 */
    @GetMapping("/ingredients/categories")
    public ApiResponse<?> categories() {
        return ApiResponse.ok(ingredientService.listCategoriesWithIngredients());
    }

    /** 食材关键词搜索（自动补全） */
    @GetMapping("/ingredients/search")
    public ApiResponse<List<String>> search(@RequestParam String keyword) {
        return ApiResponse.ok(ingredientService.searchIngredients(keyword));
    }

    /** 获取所有可选菜系（中餐 + 国际料理） */
    @GetMapping("/cuisines")
    public ApiResponse<List<Cuisine>> cuisines() {
        return ApiResponse.ok(ingredientService.listCuisines());
    }

    /**
     * MiMo 图像识别：上传食材照片，返回识别出的食材名称列表。
     * POST /api/ingredients/recognize，表单字段名 file
     */
    @PostMapping("/ingredients/recognize")
    public ApiResponse<List<String>> recognize(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ApiResponse.fail("请上传图片");
        }
        List<String> ingredients = mimoVisionService.recognizeIngredients(file);
        return ApiResponse.ok("识别成功", ingredients);
    }

    /**
     * 轮询 Agent 会话状态，前端每 2 秒调用一次。
     * GET /api/agent/session/{sessionId}
     * 当 status=completed 时停止轮询并跳转详情页；status=failed 时展示错误
     */
    @GetMapping("/agent/session/{sessionId}")
    public ApiResponse<Map<String, Object>> sessionState(@PathVariable String sessionId) {
        return ApiResponse.ok(recipeWorkflowService.getSessionState(sessionId));
    }
}
