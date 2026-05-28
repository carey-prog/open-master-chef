package org.carey.masterchef.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;

/**
 * 菜品效果图：DashScope 通义万相（异步任务）+ 本地存储，失败回退占位 SVG。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DishImageService {

    public static final String PLACEHOLDER = "/images/placeholder-dish.svg?v=2";
    private static final String BASE_URL = "https://dashscope.aliyuncs.com";

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${spring.ai.dashscope.api-key:}")
    private String dashscopeApiKey;

    @Value("${masterchef.dashscope.image-model:wanx2.1-t2i-turbo}")
    private String imageModel;

    @Value("${masterchef.images.storage-dir:./data/images}")
    private String storageDir;

    public String generateDishImage(String dishTitle, String summary) {
        if (!StringUtils.hasText(dashscopeApiKey) || dashscopeApiKey.startsWith("your-")) {
            log.warn("DashScope Key 未配置，使用占位图");
            return PLACEHOLDER;
        }
        try {
            String prompt = buildPrompt(dishTitle, summary);
            String remoteUrl = callDashScopeImageAsync(prompt);
            if (!StringUtils.hasText(remoteUrl)) {
                log.warn("万相未返回图片 URL，使用占位图");
                return PLACEHOLDER;
            }
            return downloadAndSave(remoteUrl);
        } catch (Exception e) {
            log.warn("DashScope 效果图生成失败，使用占位图: {}", e.getMessage());
            return PLACEHOLDER;
        }
    }

    private String buildPrompt(String dishTitle, String summary) {
        String prompt = "Professional food photography, Chinese cuisine on white plate, dish: "
                + dishTitle;
        if (StringUtils.hasText(summary)) {
            prompt += ", " + summary;
        }
        prompt += ", appetizing, warm lighting, restaurant quality, high detail";
        if (prompt.length() > 480) {
            prompt = prompt.substring(0, 480);
        }
        return prompt;
    }

    /**
     * 万相 2.1 需异步调用：先创建任务，再轮询 /tasks/{taskId} 获取图片 URL。
     */
    private String callDashScopeImageAsync(String prompt) throws InterruptedException {
        WebClient client = webClientBuilder.baseUrl(BASE_URL).build();

        Map<?, ?> createResp = client.post()
                .uri("/api/v1/services/aigc/text2image/image-synthesis")
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + dashscopeApiKey)
                .header("X-DashScope-Async", "enable")
                .contentType(MediaType.APPLICATION_JSON)
                .bodyValue(Map.of(
                        "model", imageModel,
                        "input", Map.of("prompt", prompt),
                        "parameters", Map.of("size", "1024*1024", "n", 1)
                ))
                .retrieve()
                .bodyToMono(Map.class)
                .block();

        if (createResp == null) {
            return null;
        }
        JsonNode root = objectMapper.valueToTree(createResp);
        String taskId = root.path("output").path("task_id").asText(null);
        if (!StringUtils.hasText(taskId)) {
            String code = root.path("code").asText("");
            String message = root.path("message").asText("");
            log.warn("万相创建任务失败 model={} code={} message={} resp={}",
                    imageModel, code, message, createResp);
            return null;
        }
        log.info("万相任务已创建 taskId={}", taskId);

        // 轮询最多 90 秒
        for (int i = 0; i < 45; i++) {
            Thread.sleep(2000);
            Map<?, ?> taskResp = client.get()
                    .uri("/api/v1/tasks/{taskId}", taskId)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + dashscopeApiKey)
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            if (taskResp == null) {
                continue;
            }
            JsonNode taskNode = objectMapper.valueToTree(taskResp);
            String status = taskNode.path("output").path("task_status").asText("");
            if ("SUCCEEDED".equals(status)) {
                JsonNode urlNode = taskNode.path("output").path("results").path(0).path("url");
                if (urlNode.isTextual() && StringUtils.hasText(urlNode.asText())) {
                    log.info("万相图片生成成功");
                    return urlNode.asText();
                }
                log.warn("万相任务成功但无 URL: {}", taskResp);
                return null;
            }
            if ("FAILED".equals(status) || "CANCELED".equals(status)) {
                log.warn("万相任务失败 status={} resp={}", status, taskResp);
                return null;
            }
        }
        log.warn("万相任务轮询超时 taskId={}", taskId);
        return null;
    }

    private String downloadAndSave(String remoteUrl) throws Exception {
        Path dir = Paths.get(storageDir).toAbsolutePath().normalize();
        Files.createDirectories(dir);
        String filename = UUID.randomUUID().toString().replace("-", "") + ".png";
        Path target = dir.resolve(filename);

        byte[] bytes = webClientBuilder.build()
                .get()
                .uri(remoteUrl)
                .retrieve()
                .bodyToMono(byte[].class)
                .block(Duration.ofSeconds(60));

        if (bytes == null || bytes.length == 0) {
            return PLACEHOLDER;
        }
        Files.write(target, bytes);
        log.info("菜品图已保存 {}", target);
        return "/images/generated/" + filename;
    }
}
