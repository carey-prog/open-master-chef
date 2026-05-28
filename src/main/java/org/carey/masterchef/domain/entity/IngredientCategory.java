package org.carey.masterchef.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("ingredient_category")
public class IngredientCategory {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String name;
    private String emoji;
    private Integer sortOrder;
    private LocalDateTime createdAt;
}
