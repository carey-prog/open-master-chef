package org.carey.masterchef.domain.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;
import java.util.Map;

@Data
@Builder
public class RecipeDetailDto {
    private Long id;
    private String sessionId;
    private String title;
    private String cuisineName;
    private String difficulty;
    private Integer cookingTime;
    private String summary;
    private String imageUrl;
    private List<String> inputIngredients;
    private List<IngredientItem> ingredients;
    private List<String> steps;
    private Map<String, Object> nutrition;
    private String status;
    private String message;

    @Data
    @Builder
    public static class IngredientItem {
        private String category;
        private String name;
        private String amount;
    }
}
