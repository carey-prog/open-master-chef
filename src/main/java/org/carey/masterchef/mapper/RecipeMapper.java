package org.carey.masterchef.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.carey.masterchef.domain.entity.Recipe;

@Mapper
public interface RecipeMapper extends BaseMapper<Recipe> {
}
