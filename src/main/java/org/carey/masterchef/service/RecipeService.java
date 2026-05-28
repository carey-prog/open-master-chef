package org.carey.masterchef.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.carey.masterchef.domain.dto.GeneratedRecipePayload;
import org.carey.masterchef.domain.dto.RecipeDetailDto;
import org.carey.masterchef.domain.entity.Recipe;
import org.carey.masterchef.domain.entity.RecipeIngredient;
import org.carey.masterchef.domain.entity.RecipeStep;
import org.carey.masterchef.mapper.RecipeIngredientMapper;
import org.carey.masterchef.mapper.RecipeMapper;
import org.carey.masterchef.mapper.RecipeStepMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecipeService {

    /** TEXT/MEDIUMTEXT 入库前的安全上限，防止异常大上下文导致写入失败 */
    private static final int MAX_STORED_CONTEXT_CHARS = 16000;

    private final RecipeMapper recipeMapper;
    private final RecipeIngredientMapper recipeIngredientMapper;
    private final RecipeStepMapper recipeStepMapper;
    private final ObjectMapper objectMapper;

    @Transactional
    public Long saveGeneratedRecipe(String sessionId,
                                    List<String> inputIngredients,
                                    String cuisineCode,
                                    String cuisineName,
                                    String customRequire,
                                    String ragContext,
                                    String webContext,
                                    GeneratedRecipePayload payload,
                                    Map<String, Object> nutrition,
                                    String imageUrl) {
        try {
            Recipe recipe = new Recipe();
            recipe.setSessionId(sessionId);
            recipe.setTitle(payload.getTitle());
            recipe.setCuisineCode(cuisineCode);
            recipe.setCuisineName(cuisineName);
            recipe.setDifficulty(payload.getDifficulty());
            recipe.setCookingTime(payload.getCookingTime());
            recipe.setCustomRequire(customRequire);
            recipe.setSummary(payload.getSummary());
            recipe.setImageUrl(imageUrl);
            recipe.setInputIngredients(objectMapper.writeValueAsString(inputIngredients));
            recipe.setRagContext(limitContextLength(ragContext));
            recipe.setWebContext(limitContextLength(webContext));
            recipe.setNutritionJson(objectMapper.writeValueAsString(nutrition));
            recipe.setStatus("completed");
            recipeMapper.insert(recipe);

            int sort = 0;
            for (GeneratedRecipePayload.IngredientItem item : payload.getIngredients()) {
                RecipeIngredient ri = new RecipeIngredient();
                ri.setRecipeId(recipe.getId());
                ri.setCategory(item.getCategory());
                ri.setName(item.getName());
                ri.setAmount(item.getAmount());
                ri.setSortOrder(sort++);
                recipeIngredientMapper.insert(ri);
            }

            int stepNum = 1;
            for (String step : payload.getSteps()) {
                RecipeStep rs = new RecipeStep();
                rs.setRecipeId(recipe.getId());
                rs.setStepNumber(stepNum++);
                rs.setDescription(step);
                recipeStepMapper.insert(rs);
            }
            return recipe.getId();
        } catch (Exception e) {
            throw new IllegalStateException("保存菜谱失败", e);
        }
    }

    private String limitContextLength(String context) {
        if (context == null || context.length() <= MAX_STORED_CONTEXT_CHARS) {
            return context;
        }
        return context.substring(0, MAX_STORED_CONTEXT_CHARS) + "\n...(truncated)";
    }

    public RecipeDetailDto getRecipeDetail(Long id) {
        Recipe recipe = recipeMapper.selectById(id);
        if (recipe == null) {
            return null;
        }
        List<RecipeIngredient> ingredients = recipeIngredientMapper.selectList(
                new LambdaQueryWrapper<RecipeIngredient>()
                        .eq(RecipeIngredient::getRecipeId, id)
                        .orderByAsc(RecipeIngredient::getSortOrder));
        List<RecipeStep> steps = recipeStepMapper.selectList(
                new LambdaQueryWrapper<RecipeStep>()
                        .eq(RecipeStep::getRecipeId, id)
                        .orderByAsc(RecipeStep::getStepNumber));

        try {
            List<String> inputList = objectMapper.readValue(
                    recipe.getInputIngredients() != null ? recipe.getInputIngredients() : "[]",
                    new TypeReference<List<String>>() {});
            Map<String, Object> nutrition = objectMapper.readValue(
                    recipe.getNutritionJson() != null ? recipe.getNutritionJson() : "{}",
                    new TypeReference<Map<String, Object>>() {});

            return RecipeDetailDto.builder()
                    .id(recipe.getId())
                    .sessionId(recipe.getSessionId())
                    .title(recipe.getTitle())
                    .cuisineName(recipe.getCuisineName())
                    .difficulty(recipe.getDifficulty())
                    .cookingTime(recipe.getCookingTime())
                    .summary(recipe.getSummary())
                    .imageUrl(recipe.getImageUrl())
                    .inputIngredients(inputList)
                    .ingredients(ingredients.stream()
                            .map(i -> RecipeDetailDto.IngredientItem.builder()
                                    .category(i.getCategory())
                                    .name(i.getName())
                                    .amount(i.getAmount())
                                    .build())
                            .toList())
                    .steps(steps.stream().map(RecipeStep::getDescription).toList())
                    .nutrition(nutrition)
                    .status(recipe.getStatus())
                    .build();
        } catch (Exception e) {
            log.error("解析菜谱详情失败", e);
            return RecipeDetailDto.builder()
                    .id(recipe.getId())
                    .title(recipe.getTitle())
                    .status("error")
                    .message("解析菜谱数据失败")
                    .build();
        }
    }
}
