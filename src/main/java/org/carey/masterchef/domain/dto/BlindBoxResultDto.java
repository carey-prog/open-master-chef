package org.carey.masterchef.domain.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class BlindBoxResultDto {
    private List<String> ingredients;
    private String cuisineCode;
    private String cuisineName;
    private String cuisineEmoji;
    private String chefTitle;
    private String chefDesc;
    private String preference;
}
