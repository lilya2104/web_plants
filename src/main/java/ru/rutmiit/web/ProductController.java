package ru.rutmiit.web;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Controller;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.rutmiit.dto.AddProductDto;
import ru.rutmiit.dto.ShowProductInfoDto;
import ru.rutmiit.models.enums.*;
import ru.rutmiit.services.OrderService;
import ru.rutmiit.services.ProductService;

import java.security.Principal;

@Slf4j
@Controller
@RequestMapping("/products")
public class ProductController {

    private final ProductService productService;
    private final OrderService orderService;

    public ProductController(ProductService productService, OrderService orderService) {
        this.productService = productService;
        this.orderService = orderService;
        log.info("ProductController инициализирован");
    }

    @GetMapping("/add")
    public String addProduct(Model model) {
        log.debug("Отображение формы добавления растения");
        model.addAttribute("careLevels", CareLevel.values());
        model.addAttribute("lightRequirements", LightRequirement.values());
        model.addAttribute("wateringFrequencies", WateringFrequency.values());
        model.addAttribute("growthRates", GrowthRate.values());
        model.addAttribute("sizePlants", SizePlant.values());
        return "product-add";
    }

    // автоматически создает пустой объект AddProductDto
    // делает его доступным в шаблоне под именем productModel
    // это нужно для привязки полей формы к полям объекта
    @ModelAttribute("productModel")
    public AddProductDto initProduct() {
        return new AddProductDto();
    }

    // @Valid - активирует валидацию по аннотациям в дто
    @PostMapping("/add")
    public String addProduct(@Valid AddProductDto productModel, BindingResult bindingResult,
                             RedirectAttributes redirectAttributes) {
        log.debug("Обработка POST запроса на добавление растения");
        if (bindingResult.hasErrors()) {
            log.warn("Ошибки валидации при добавлении растения: {}", bindingResult.getAllErrors()); // BindingResult - содержит результаты валидации
            redirectAttributes.addFlashAttribute("careLevels", CareLevel.values());
            redirectAttributes.addFlashAttribute("lightRequirements", LightRequirement.values());
            redirectAttributes.addFlashAttribute("wateringFrequencies", WateringFrequency.values());
            redirectAttributes.addFlashAttribute("growthRates", GrowthRate.values());
            redirectAttributes.addFlashAttribute("sizePlants", SizePlant.values());
            redirectAttributes.addFlashAttribute("productModel", productModel); // остальные характеристики растения
            redirectAttributes.addFlashAttribute(
                    "org.springframework.validation.BindingResult.productModel",
                    bindingResult);
            return "redirect:/products/add";
        }
        productService.addProduct(productModel);
        redirectAttributes.addFlashAttribute("successMessage",
                "Растение '" + productModel.getName() + "' успешно добавлено!");
        return "redirect:/products";
    }

    @GetMapping("")
    public String showAllProducts(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "2") int size,
            @RequestParam(defaultValue = "name") String sortBy,
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) CareLevel careLevel,
            @RequestParam(required = false) LightRequirement lightRequirement,
            @RequestParam(required = false) WateringFrequency wateringFrequency,
            @RequestParam(required = false) GrowthRate growthRate,
            @RequestParam(required = false) SizePlant sizePlant,
            @RequestParam(required = false) Boolean petSafe, // остальные характеристики растения
            Model model) {

        log.debug("Отображение списка растений: страница={}, размер={}, сортировка={}, поиск={}",
                page, size, sortBy, searchTerm);
        model.addAttribute("careLevels", CareLevel.values());
        model.addAttribute("lightRequirements", LightRequirement.values());
        model.addAttribute("wateringFrequencies", WateringFrequency.values());
        model.addAttribute("growthRates", GrowthRate.values());
        model.addAttribute("sizePlants", SizePlant.values()); // остальные характеристики растения

        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            model.addAttribute("allProducts", productService.searchProduct(searchTerm));
            model.addAttribute("searchTerm", searchTerm);
        }
        else if (careLevel != null || lightRequirement != null || wateringFrequency != null ||
                growthRate != null || sizePlant != null || petSafe != null) {

            model.addAttribute("allProducts", productService.findProductsByProperties(
                    careLevel, lightRequirement, wateringFrequency, growthRate, sizePlant, petSafe));
            model.addAttribute("selectedCareLevel", careLevel);
            model.addAttribute("selectedLightRequirement", lightRequirement);
            model.addAttribute("selectedWateringFrequency", wateringFrequency);
            model.addAttribute("selectedGrowthRate", growthRate);
            model.addAttribute("selectedSizePlant", sizePlant);
            model.addAttribute("selectedPetSafe", petSafe); // остальные выбранные параметры
        }
        else {
            Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).ascending());
            Page<ShowProductInfoDto> productPage = productService.allProductsPaginated(pageable);

            model.addAttribute("allProducts", productPage.getContent());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", productPage.getTotalPages());
            model.addAttribute("totalItems", productPage.getTotalElements());
        }
        return "product-all";
    }

    @GetMapping("/{productName}")
    public String showProductDetails(@PathVariable String productName, Model model) {
        log.debug("Отображение деталей растения: {}", productName);
        model.addAttribute("productDetails", productService.productDetails(productName));
        return "product-details";
    }

    @PostMapping("/order/{productName}")
    public String buyProduct(@PathVariable String productName,
                             @RequestParam(defaultValue = "1") int count,
                             Principal principal,
                             RedirectAttributes redirectAttributes) {
        orderService.createOrder(principal.getName(), productName, count);
        redirectAttributes.addFlashAttribute("successMessage", "Заказ успешно оформлен!");
        return "redirect:/users/profile";
    }

    @GetMapping("/delete/{productName}")
    public String deleteProduct(@PathVariable String productName, RedirectAttributes redirectAttributes) {
        log.debug("Удаление растения: {}", productName);
        try {
            productService.removeProduct(productName);
            redirectAttributes.addFlashAttribute("successMessage",
                    "Растение '" + productName + "' успешно удалено!");
        } catch (Exception e) {
            redirectAttributes.addFlashAttribute("errorMessage",
                    "Ошибка при удалении растения: " + e.getMessage());
        }
        return "redirect:/products";
    }

}
