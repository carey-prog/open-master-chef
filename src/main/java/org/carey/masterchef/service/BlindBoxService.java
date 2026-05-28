package org.carey.masterchef.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.carey.masterchef.domain.dto.BlindBoxResultDto;
import org.carey.masterchef.domain.entity.Cuisine;
import org.carey.masterchef.domain.entity.Ingredient;
import org.carey.masterchef.domain.entity.IngredientCategory;
import org.carey.masterchef.mapper.CuisineMapper;
import org.carey.masterchef.mapper.IngredientCategoryMapper;
import org.carey.masterchef.mapper.IngredientMapper;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

/**
 * 美食盲盒：按偏好随机抽取 6 种食材 + 1 位菜系大师。
 */
@Service
@RequiredArgsConstructor
public class BlindBoxService {

    private static final int PICK_COUNT = 6;

    private static final Set<String> MEAT_CATEGORIES = Set.of("荤菜", "海鲜");
    private static final Set<String> VEG_CATEGORIES = Set.of("蔬菜", "菌菇", "豆类", "水果", "坚果");

    private static final Map<String, String> CHEF_DESC = Map.ofEntries(
            Map.entry("su", "清鲜平和，讲究本味"),
            Map.entry("lu", "咸鲜为主，火候精湛"),
            Map.entry("chuan", "麻辣鲜香，百菜百味"),
            Map.entry("yue", "清鲜爽嫩，原汁原味"),
            Map.entry("zhe", "清香软嫩，注重时令"),
            Map.entry("xiang", "香辣酸爽，口味浓郁"),
            Map.entry("min", "清鲜和醇，擅用红糟"),
            Map.entry("hui", "重油重色，火功独到"),
            Map.entry("japanese", "简约精致，追求本味"),
            Map.entry("korean", "发酵调味，层次丰富"),
            Map.entry("italian", "橄榄油香，番茄基底"),
            Map.entry("french", "精致优雅，酱汁见长"),
            Map.entry("indian", "香料丰富，层次复杂"),
            Map.entry("thai", "酸辣平衡，香料突出"),
            Map.entry("mexican", "辣椒丰富，玉米文化")
    );

    private final IngredientCategoryMapper categoryMapper;
    private final IngredientMapper ingredientMapper;
    private final CuisineMapper cuisineMapper;

    public BlindBoxResultDto randomPick(String preference) {
        String pref = preference != null ? preference : "balanced";
        List<String> pool = buildPool(pref);
        if (pool.isEmpty()) {
            pool = allIngredientNames();
        }
        Collections.shuffle(pool, ThreadLocalRandom.current());
        List<String> picked = pool.stream().distinct().limit(PICK_COUNT).collect(Collectors.toList());
        while (picked.size() < PICK_COUNT && pool.size() > picked.size()) {
            String extra = pool.get(ThreadLocalRandom.current().nextInt(pool.size()));
            if (!picked.contains(extra)) {
                picked.add(extra);
            }
        }

        List<Cuisine> cuisines = cuisineMapper.selectList(new LambdaQueryWrapper<Cuisine>().orderByAsc(Cuisine::getSortOrder));
        Cuisine chef = cuisines.get(ThreadLocalRandom.current().nextInt(cuisines.size()));

        return BlindBoxResultDto.builder()
                .ingredients(picked)
                .cuisineCode(chef.getCode())
                .cuisineName(chef.getName())
                .cuisineEmoji(chef.getEmoji())
                .chefTitle(chef.getName() + "料理大师")
                .chefDesc(CHEF_DESC.getOrDefault(chef.getCode(), "匠心烹饪，创意无限"))
                .preference(pref)
                .build();
    }

    private List<String> buildPool(String preference) {
        return switch (preference) {
            case "meat_heavy" -> mixCategories(MEAT_CATEGORIES, 4, VEG_CATEGORIES, 2);
            case "veg_heavy" -> mixCategories(VEG_CATEGORIES, 4, MEAT_CATEGORIES, 2);
            case "vegan" -> namesByCategories(VEG_CATEGORIES);
            case "all_meat" -> namesByCategories(MEAT_CATEGORIES);
            default -> allIngredientNames();
        };
    }

    private List<String> mixCategories(Set<String> primary, int primaryCount,
                                       Set<String> secondary, int secondaryCount) {
        List<String> result = new ArrayList<>();
        List<String> primaryPool = namesByCategories(primary);
        List<String> secondaryPool = namesByCategories(secondary);
        Collections.shuffle(primaryPool);
        Collections.shuffle(secondaryPool);
        result.addAll(primaryPool.stream().limit(primaryCount).toList());
        result.addAll(secondaryPool.stream().limit(secondaryCount).toList());
        return result;
    }

    private List<String> namesByCategories(Set<String> categoryNames) {
        List<IngredientCategory> cats = categoryMapper.selectList(
                new LambdaQueryWrapper<IngredientCategory>().in(IngredientCategory::getName, categoryNames));
        if (cats.isEmpty()) {
            return List.of();
        }
        List<Long> ids = cats.stream().map(IngredientCategory::getId).toList();
        return ingredientMapper.selectList(
                        new LambdaQueryWrapper<Ingredient>().in(Ingredient::getCategoryId, ids))
                .stream().map(Ingredient::getName).distinct().collect(Collectors.toList());
    }

    private List<String> allIngredientNames() {
        return ingredientMapper.selectList(new LambdaQueryWrapper<>())
                .stream().map(Ingredient::getName).distinct().collect(Collectors.toList());
    }
}
