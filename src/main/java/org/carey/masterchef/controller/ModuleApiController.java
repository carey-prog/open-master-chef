package org.carey.masterchef.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.carey.masterchef.domain.dto.ApiResponse;
import org.carey.masterchef.domain.dto.BlindBoxResultDto;
import org.carey.masterchef.domain.dto.FeastGenerateRequest;
import org.carey.masterchef.domain.dto.FeastMenuResultDto;
import org.carey.masterchef.domain.dto.RecipeGenerateRequest;
import org.carey.masterchef.service.BlindBoxService;
import org.carey.masterchef.service.FeastMenuService;
import org.carey.masterchef.service.RecipeWorkflowService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

/**
 * 扩展模块 API：美食盲盒、满汉全席。
 */
@RestController
@RequestMapping("/api/modules")
@RequiredArgsConstructor
public class ModuleApiController {

    private final BlindBoxService blindBoxService;
    private final FeastMenuService feastMenuService;
    private final RecipeWorkflowService recipeWorkflowService;

    /** 美食盲盒：随机抽取食材 + 推荐主厨 */
    @PostMapping("/blind-box/random")
    public ApiResponse<BlindBoxResultDto> blindBoxRandom(@RequestParam(defaultValue = "balanced") String preference) {
        return ApiResponse.ok(blindBoxService.randomPick(preference));
    }

    /** 满汉全席：生成一桌菜菜单方案 */
    @PostMapping("/feast/generate")
    public ApiResponse<FeastMenuResultDto> generateFeast(@Valid @RequestBody FeastGenerateRequest request) {
        return ApiResponse.ok(feastMenuService.generateMenu(request));
    }

    /**
     * 盲盒结果一键生成菜谱（复用现有 Graph 工作流）。
     */
    @PostMapping("/blind-box/generate-recipe")
    public ApiResponse<Map<String, Object>> blindBoxGenerateRecipe(@RequestBody RecipeGenerateRequest request) {
        String sessionId = recipeWorkflowService.startGenerate(request);
        return ApiResponse.ok("已开始生成", Map.of("sessionId", sessionId, "status", "running"));
    }
}
