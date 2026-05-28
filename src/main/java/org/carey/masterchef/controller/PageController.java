package org.carey.masterchef.controller;

import lombok.RequiredArgsConstructor;
import org.carey.masterchef.domain.entity.Cuisine;
import org.carey.masterchef.service.IngredientService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 页面路由控制器（Thymeleaf 服务端渲染）。
 */
@Controller
@RequiredArgsConstructor
public class PageController {

    private final IngredientService ingredientService;

    @GetMapping({"/", "/index"})
    public String index(Model model) {
        fillCommonModel(model, "home");
        model.addAttribute("categories", ingredientService.listCategoriesWithIngredients());
        List<Cuisine> cuisines = ingredientService.listCuisines();
        model.addAttribute("chineseCuisines", filterCuisines(cuisines, "chinese"));
        model.addAttribute("internationalCuisines", filterCuisines(cuisines, "international"));
        return "index";
    }

    @GetMapping("/blind-box")
    public String blindBox(Model model) {
        fillCommonModel(model, "blind-box");
        return "blind-box";
    }

    @GetMapping("/feast")
    public String feast(Model model) {
        fillCommonModel(model, "feast");
        return "feast";
    }

    @GetMapping("/mystical-kitchen")
    public String mysticalKitchen(Model model) {
        fillCommonModel(model, "mystical");
        model.addAttribute("moduleTitle", "玄学厨房");
        model.addAttribute("moduleDesc", "敬请期待：塔罗牌选菜、星座幸运菜即将上线");
        return "coming-soon";
    }

    @GetMapping("/sauce-master")
    public String sauceMaster(Model model) {
        fillCommonModel(model, "sauce");
        model.addAttribute("moduleTitle", "酱料大师");
        model.addAttribute("moduleDesc", "敬请期待：万能蘸料、灵魂酱汁配方即将上线");
        return "coming-soon";
    }

    @GetMapping("/recipe/{id}")
    public String recipeDetail(@PathVariable Long id, Model model) {
        fillCommonModel(model, "home");
        model.addAttribute("recipeId", id);
        return "recipe-detail";
    }

    private void fillCommonModel(Model model, String activeNav) {
        model.addAttribute("activeNav", activeNav);
    }

    private List<Cuisine> filterCuisines(List<Cuisine> cuisines, String groupType) {
        return cuisines.stream()
                .filter(c -> groupType.equals(c.getGroupType()))
                .collect(Collectors.toList());
    }
}
