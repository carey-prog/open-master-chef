package org.carey.masterchef.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class RecipeGenerateRequest {
    @NotEmpty(message = "请至少选择一种食材")
    @Size(max = 10, message = "最多选择10种食材")
    private List<String> ingredients;

    @NotBlank(message = "请选择菜系")
    private String cuisineCode;

    private String customRequire;
    private String sessionId;
}
