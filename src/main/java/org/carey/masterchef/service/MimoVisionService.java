package org.carey.masterchef.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.carey.masterchef.config.MimoProperties;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * 小米 MiMo 多模态 AI 服务：图像识别 + 菜品效果图。
 * <p>
 * MiMo API 兼容 OpenAI Chat Completions 格式，支持文本 + 图片的多模态输入。
 * 配置项见 application.yml 的 {@code masterchef.mimo.*}。
 * </p>
 *
 * <p>在本项目中的两个用途：</p>
 * <ul>
 *   <li><b>recognizeIngredients</b> — 首页「拍照识食材」功能</li>
 *   <li><b>generateDishImage</b> — Graph 工作流中生成菜品效果图（当前返回占位图）</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MimoVisionService {

    private final MimoProperties mimoProperties;
    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    /**
     * 识别上传图片中的食材。
     * <p>
     * 流程：图片 → Base64 编码 → 构造多模态 Prompt（文本 + image_url）→ 调用 MiMo API → 解析 JSON 数组
     * </p>
     *
     * @param image 用户上传的图片文件
     * @return 识别出的食材名称列表，如 ["白菜", "鸡蛋"]
     */
    public List<String> recognizeIngredients(MultipartFile image) {
        if (!StringUtils.hasText(mimoProperties.getApiKey())
                || mimoProperties.getApiKey().startsWith("your-")) {
            log.warn("MiMo API Key 未配置，返回演示数据");
            return List.of("白菜", "胡萝卜", "鸡蛋");
        }
        try {
            // 将图片转为 Base64 Data URL，MiMo 支持这种 inline 图片格式
            String base64 = Base64.getEncoder().encodeToString(image.getBytes());
            String mime = image.getContentType() != null ? image.getContentType() : "image/jpeg";
            String dataUrl = "data:" + mime + ";base64," + base64;

            WebClient client = webClientBuilder.baseUrl(mimoProperties.getBaseUrl()).build();
            Map<?, ?> response = client.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + mimoProperties.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of(
                            "model", mimoProperties.getVisionModel(),
                            "messages", List.of(Map.of(
                                    "role", "user",
                                    "content", List.of(
                                            Map.of("type", "text", "text",
                                                    "请识别图片中的食材，只返回 JSON 数组，例如 [\"白菜\",\"鸡蛋\"]，不要其他文字。"),
                                            Map.of("type", "image_url", "image_url", Map.of("url", dataUrl))
                                    )
                            )),
                            "max_completion_tokens", 512,
                            "temperature", 0.2  // 低温度，提高识别准确性
                    ))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();

            String content = extractContent(response);
            return parseIngredientList(content);
        } catch (Exception e) {
            log.error("MiMo 图像识别失败", e);
            throw new IllegalStateException("图像识别失败: " + e.getMessage());
        }
    }

    /**
     * 为生成的菜谱创建效果图 URL。
     * <p>
     * 当前实现：调用 MiMo 生成英文 prompt 描述，但返回占位 SVG 路径。
     * 后续可对接 DALL-E / Stable Diffusion 等图像生成 API 替换占位图。
     * </p>
     */
    public String generateDishImage(String dishTitle, String summary) {
        if (!StringUtils.hasText(mimoProperties.getApiKey())
                || mimoProperties.getApiKey().startsWith("your-")) {
            return DishImageService.PLACEHOLDER;
        }
        try {
            WebClient client = webClientBuilder.baseUrl(mimoProperties.getBaseUrl()).build();
            Map<?, ?> response = client.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + mimoProperties.getApiKey())
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(Map.of(
                            "model", mimoProperties.getImageModel(),
                            "messages", List.of(Map.of(
                                    "role", "user",
                                    "content", "请为中式菜品「" + dishTitle + "」生成一张精美效果图的描述，"
                                            + "并给出可用于图像生成的英文 prompt，格式：PROMPT: ..."
                                            + " 菜品简介：" + summary
                            )),
                            "max_completion_tokens", 256,
                            "temperature", 0.8
                    ))
                    .retrieve()
                    .bodyToMono(Map.class)
                    .block();
            String content = extractContent(response);
            log.info("MiMo 效果图描述: {}", content);
            return DishImageService.PLACEHOLDER;
        } catch (Exception e) {
            log.warn("MiMo 效果图生成失败: {}", e.getMessage());
            return DishImageService.PLACEHOLDER;
        }
    }

    /** 从 OpenAI 兼容格式的响应中提取 assistant 的 content 文本 */
    @SuppressWarnings("unchecked")
    private String extractContent(Map<?, ?> response) {
        if (response == null) {
            return "";
        }
        List<Map<String, Object>> choices = (List<Map<String, Object>>) response.get("choices");
        if (choices == null || choices.isEmpty()) {
            return "";
        }
        Map<String, Object> message = (Map<String, Object>) choices.get(0).get("message");
        return message != null ? String.valueOf(message.get("content")) : "";
    }

    /** 解析 MiMo 返回的 JSON 数组；解析失败时用逗号分割兜底 */
    private List<String> parseIngredientList(String content) {
        try {
            String json = content.trim();
            int start = json.indexOf('[');
            int end = json.lastIndexOf(']');
            if (start >= 0 && end > start) {
                json = json.substring(start, end + 1);
            }
            JsonNode node = objectMapper.readTree(json);
            List<String> list = new ArrayList<>();
            node.forEach(n -> list.add(n.asText()));
            return list;
        } catch (Exception e) {
            log.warn("解析 MiMo 识别结果失败，原始内容: {}", content);
            return List.of(content.replaceAll("[\\[\\]\"]", "").split("[,，、]")).stream()
                    .map(String::trim)
                    .filter(StringUtils::hasText)
                    .limit(10)
                    .toList();
        }
    }
}
