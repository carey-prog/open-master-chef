package org.carey.masterchef.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.carey.masterchef.domain.dto.FeastGenerateRequest;
import org.carey.masterchef.domain.dto.FeastMenuResultDto;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

/**
 * 满汉全席 / 一桌好菜师：根据模式与偏好生成整桌菜单方案。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class FeastMenuService {

    private final ChatClient deepSeekChatClient;
    private final ObjectMapper objectMapper;

    public FeastMenuResultDto generateMenu(FeastGenerateRequest request) {
        validate(request);
        String prompt = buildPrompt(request);
        log.info("调用 DeepSeek 生成一桌菜 menu mode={} count={}", request.getMode(), request.getDishCount());
        String response = deepSeekChatClient.prompt().user(prompt).call().content();
        return parseMenuJson(response, request.getDishCount());
    }

    private void validate(FeastGenerateRequest request) {
        if ("smart".equals(request.getMode())
                && (request.getSpecifiedDishes() == null || request.getSpecifiedDishes().isEmpty())) {
            throw new IllegalArgumentException("智能搭配模式需要至少输入一道菜品");
        }
    }

    private String buildPrompt(FeastGenerateRequest req) {
        String modeDesc = "smart".equals(req.getMode())
                ? "智能搭配模式：请根据用户指定菜品，AI 决定总道数与荤素搭配"
                : "固定数量模式：请严格生成 " + req.getDishCount() + " 道菜";

        return """
                你是一位国宴级菜单设计大师。请设计一桌完整的宴席菜单。
                
                【生成模式】%s
                【指定菜品】%s
                【口味偏好】%s
                【菜系风格】%s
                【用餐场景】%s
                【营养搭配】%s
                【特殊要求】%s
                
                请严格返回 JSON，不要 markdown，格式：
                {
                  "menuTitle": "菜单主题名",
                  "summary": "整桌菜一句话概述",
                  "dishes": [
                    {"name":"菜名","type":"主菜/汤/凉菜/主食","brief":"一句话介绍","difficulty":"简单/中等/困难"}
                  ]
                }
                """.formatted(
                modeDesc,
                joinOrDefault(req.getSpecifiedDishes(), "无"),
                joinOrDefault(req.getTastePrefs(), "不限"),
                nullToDefault(req.getCuisineStyle(), "混合菜系"),
                nullToDefault(req.getDiningScene(), "家庭聚餐"),
                nullToDefault(req.getNutritionPref(), "营养均衡"),
                nullToDefault(req.getSpecialRequire(), "无")
        );
    }

    private FeastMenuResultDto parseMenuJson(String response, int expectedCount) {
        try {
            String json = extractJson(response);
            FeastMenuResultDto result = objectMapper.readValue(json, FeastMenuResultDto.class);
            if (result.getDishes() == null || result.getDishes().isEmpty()) {
                throw new IllegalStateException("菜单为空");
            }
            return result;
        } catch (Exception e) {
            log.error("解析菜单 JSON 失败: {}", response, e);
            throw new IllegalStateException("菜单生成格式异常，请重试");
        }
    }

    private String extractJson(String text) {
        String trimmed = text.trim();
        int start = trimmed.indexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        return trimmed;
    }

    private String joinOrDefault(List<String> list, String def) {
        return list == null || list.isEmpty() ? def : String.join("、", list);
    }

    private String nullToDefault(String s, String def) {
        return s == null || s.isBlank() ? def : s;
    }
}
