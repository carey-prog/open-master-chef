package org.carey.masterchef.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.carey.masterchef.domain.dto.IngredientCategoryDto;
import org.carey.masterchef.domain.entity.Cuisine;
import org.carey.masterchef.domain.entity.Ingredient;
import org.carey.masterchef.domain.entity.IngredientCategory;
import org.carey.masterchef.mapper.CuisineMapper;
import org.carey.masterchef.mapper.IngredientCategoryMapper;
import org.carey.masterchef.mapper.IngredientMapper;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class IngredientService {

    private final IngredientCategoryMapper categoryMapper;
    private final IngredientMapper ingredientMapper;
    private final CuisineMapper cuisineMapper;

    public List<IngredientCategoryDto> listCategoriesWithIngredients() {
        List<IngredientCategory> categories = categoryMapper.selectList(
                new LambdaQueryWrapper<IngredientCategory>().orderByAsc(IngredientCategory::getSortOrder));
        return categories.stream().map(cat -> {
            List<Ingredient> items = ingredientMapper.selectList(
                    new LambdaQueryWrapper<Ingredient>()
                            .eq(Ingredient::getCategoryId, cat.getId())
                            .orderByAsc(Ingredient::getSortOrder));
            return IngredientCategoryDto.builder()
                    .id(cat.getId())
                    .name(cat.getName())
                    .emoji(cat.getEmoji())
                    .ingredients(items.stream()
                            .map(i -> IngredientCategoryDto.IngredientTagDto.builder()
                                    .id(i.getId())
                                    .name(i.getName())
                                    .build())
                            .collect(Collectors.toList()))
                    .build();
        }).collect(Collectors.toList());
    }

    public List<Cuisine> listCuisines() {
        return cuisineMapper.selectList(new LambdaQueryWrapper<Cuisine>().orderByAsc(Cuisine::getSortOrder));
    }

    public List<String> searchIngredients(String keyword) {
        if (keyword == null || keyword.isBlank()) {
            return List.of();
        }
        return ingredientMapper.selectList(new LambdaQueryWrapper<Ingredient>()
                        .like(Ingredient::getName, keyword.trim())
                        .last("LIMIT 10"))
                .stream()
                .map(Ingredient::getName)
                .distinct()
                .collect(Collectors.toList());
    }

    public Cuisine getCuisineByCode(String code) {
        return cuisineMapper.selectOne(new LambdaQueryWrapper<Cuisine>().eq(Cuisine::getCode, code));
    }
}
