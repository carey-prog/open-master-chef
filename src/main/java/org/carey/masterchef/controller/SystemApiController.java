package org.carey.masterchef.controller;

import lombok.RequiredArgsConstructor;
import org.carey.masterchef.domain.dto.ApiResponse;
import org.carey.masterchef.service.AiCapabilityService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 系统诊断 API，用于检查各 AI 能力是否正常启用。
 * <p>
 * 访问 GET /api/system/capabilities 可查看 DeepSeek、RAG、Embedding 等组件的状态，
 * 便于排查「生成效果差」或「RAG 未生效」等问题。
 * </p>
 */
@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
public class SystemApiController {

    private final AiCapabilityService aiCapabilityService;

    @GetMapping("/capabilities")
    public ApiResponse<Map<String, Object>> capabilities() {
        return ApiResponse.ok(aiCapabilityService.getStatus());
    }
}
