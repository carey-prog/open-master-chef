package org.carey.masterchef.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("cuisine")
public class Cuisine {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String code;
    private String name;
    private String emoji;
    private String groupType;
    private Integer sortOrder;
    private LocalDateTime createdAt;
}
