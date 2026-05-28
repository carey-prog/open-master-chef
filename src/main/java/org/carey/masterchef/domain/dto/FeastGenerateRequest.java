package org.carey.masterchef.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.util.List;

@Data
public class FeastGenerateRequest {
    /** fixed | smart */
    private String mode = "fixed";
    @Min(2) @Max(12)
    private Integer dishCount = 6;
    @Size(max = 10)
    private List<String> specifiedDishes;
    private List<String> tastePrefs;
    private String cuisineStyle;
    private String diningScene;
    private String nutritionPref;
    @Size(max = 200)
    private String specialRequire;
}
