package org.carey.masterchef.domain.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class GeneratedRecipePayload {
    private String title;
    private String difficulty;
    private Integer cookingTime;
    private String summary;
    private List<IngredientItem> ingredients;
    private List<String> steps;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class IngredientItem {
        private String category;
        private String name;
        private String amount;
    }
}
