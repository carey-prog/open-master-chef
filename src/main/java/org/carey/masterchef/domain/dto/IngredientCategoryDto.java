package org.carey.masterchef.domain.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class IngredientCategoryDto {
    private Long id;
    private String name;
    private String emoji;
    private List<IngredientTagDto> ingredients;

    @Data
    @Builder
    public static class IngredientTagDto {
        private Long id;
        private String name;
    }
}
