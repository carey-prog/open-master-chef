package org.carey.masterchef.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("recipe_ingredient")
public class RecipeIngredient {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long recipeId;
    private String category;
    private String name;
    private String amount;
    private Integer sortOrder;
}
