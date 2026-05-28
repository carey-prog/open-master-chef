package org.carey.masterchef.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("recipe")
public class Recipe {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String sessionId;
    private String title;
    private String cuisineCode;
    private String cuisineName;
    private String difficulty;
    private Integer cookingTime;
    private String customRequire;
    private String summary;
    private String imageUrl;
    private String sourceImage;
    private String inputIngredients;
    private String ragContext;
    private String webContext;
    private String nutritionJson;
    private String status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
