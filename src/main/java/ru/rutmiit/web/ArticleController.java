package ru.rutmiit.web;

import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import ru.rutmiit.dto.AddArticleDto;
import ru.rutmiit.dto.ShowArticleInfoDto;
import ru.rutmiit.models.enums.PlantFamily;
import ru.rutmiit.services.ArticleService;
import ru.rutmiit.services.ProductService;

@Slf4j
@Controller
@RequestMapping("/articles")
public class ArticleController {
    private final ArticleService articleService;
    private final ProductService productService;

    public ArticleController(ArticleService articleService, ProductService productService) {
        this.articleService = articleService;
        this.productService = productService;
        log.info("ArticleController инициализирован");
    }

    @GetMapping("/add")
    public String addArticle(Model model) {
        log.debug("Отображение формы добавления статьи");
        model.addAttribute("products", productService.findProductsWithoutArticles());
        model.addAttribute("plantFamilies", PlantFamily.values());

        return "article-add";
    }

    // автоматически создает пустой объект AddArticleDto
    // делает его доступным в шаблоне под именем articleModel
    // это нужно для привязки полей формы к полям объекта
    @ModelAttribute("articleModel")
    public AddArticleDto initArticle() {
        return new AddArticleDto();
    }

    // @Valid - активирует валидацию по аннотациям в дто
    @PostMapping("/add")
    public String addArticle(@Valid AddArticleDto articleModel, BindingResult bindingResult,
                             RedirectAttributes redirectAttributes) {
        log.debug("Обработка добавления статьи: {}", articleModel.getTitle());
        if (bindingResult.hasErrors()) {
            log.warn("Ошибки валидации при добавлении статьи: {}", bindingResult.getAllErrors()); // BindingResult - содержит результаты валидации
            redirectAttributes.addFlashAttribute("articleModel", articleModel);
            redirectAttributes.addFlashAttribute("products",
                    productService.findProductsWithoutArticles());
            redirectAttributes.addFlashAttribute("plantFamilies", PlantFamily.values());
            redirectAttributes.addFlashAttribute(
                    "org.springframework.validation.BindingResult.articleModel",
                    bindingResult);
            return "redirect:/articles/add";
        }
        articleService.addArticle(articleModel);
        log.info("Статья успешно добавлена через контроллер");
        return "redirect:/articles";
    }

    @GetMapping("")
    public String showAllArticles(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "2") int size,
            @RequestParam(defaultValue = "title") String sortBy,
            @RequestParam(required = false) String searchTerm,
            @RequestParam(required = false) PlantFamily plantFamily,
            Model model) {
        log.debug("Отображение списка статей: страница={}, размер={}, сортировка={}, поиск={}, фильтр={}",
                page, size, sortBy, searchTerm, plantFamily);
        model.addAttribute("plantFamilies", PlantFamily.values());

        if (searchTerm != null && !searchTerm.trim().isEmpty()) {
            model.addAttribute("allArticles", articleService.searchArticles(searchTerm));
            model.addAttribute("searchTerm", searchTerm);
        }
        else if (plantFamily != null) {
            model.addAttribute("allArticles", articleService.findByPlantFamily(plantFamily));
            model.addAttribute("selectedFamily", plantFamily);
        }
        else {
            Pageable pageable = PageRequest.of(page, size, Sort.by(sortBy).ascending());
            Page<ShowArticleInfoDto> articlePage = articleService.allArticlesPaginated(pageable);

            model.addAttribute("allArticles", articlePage.getContent());
            model.addAttribute("currentPage", page);
            model.addAttribute("totalPages", articlePage.getTotalPages());
        }
        return "article-all";
    }

    @GetMapping("/article-details/{article-title}")
    public String showArticleDetails(@PathVariable("article-title") String articleTitle, Model model) {
        log.debug("Отображение полной статьи: {}", articleTitle);
        model.addAttribute("articleDetails", articleService.articleDetails(articleTitle));

        return "article-details";
    }

    @GetMapping("/delete/{article-title}")
    public String deleteArticle(@PathVariable("article-title") String articleTitle) {
        log.debug("Удаление статьи через контроллер: {}", articleTitle);
        articleService.removeArticle(articleTitle);
        log.info("Статья удалена через контроллер: {}", articleTitle);

        return "redirect:/articles";
    }
}
