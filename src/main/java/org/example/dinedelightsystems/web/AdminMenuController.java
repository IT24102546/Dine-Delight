package org.example.dinedelightsystems.web;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import org.example.dine_delight.model.MenuItem;
import org.example.dine_delight.repository.MenuItemRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/admin/menu")
public class AdminMenuController {

    private final MenuItemRepository menuItemRepository;

    public AdminMenuController(MenuItemRepository menuItemRepository) {
        this.menuItemRepository = menuItemRepository;
    }

    // ✅ DTO for Add/Edit forms
    public static class MenuForm {
        @NotBlank(message = "Name is required")
        private String name;

        private String description;

        @Min(value = 0, message = "Price must be positive")
        private Double price;

        private String photoUrl;

        private boolean available = true;

        // Getters and Setters
        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        public Double getPrice() { return price; }
        public void setPrice(Double price) { this.price = price; }
        public String getPhotoUrl() { return photoUrl; }
        public void setPhotoUrl(String photoUrl) { this.photoUrl = photoUrl; }
        public boolean isAvailable() { return available; }
        public void setAvailable(boolean available) { this.available = available; }
    }

    // ✅ Display Menu Management Page
    @GetMapping
    public String index(Model model) {
        model.addAttribute("items", menuItemRepository.findAll());
        model.addAttribute("menuForm", new MenuForm());
        return "admin/menu";
    }

    // ✅ Create a new Menu Item
    @PostMapping
    public String create(@Valid @ModelAttribute("menuForm") MenuForm form, BindingResult result) {
        if (result.hasErrors()) {
            System.out.println("⚠️ Validation failed for new item: " + result.getAllErrors());
            return "redirect:/admin/menu?error";
        }

        MenuItem item = new MenuItem();
        item.setName(form.getName());
        item.setDescription(form.getDescription());
        item.setPrice(form.getPrice() != null ? form.getPrice() : 0.0);
        item.setPhotoUrl(form.getPhotoUrl());
        item.setAvailable(form.isAvailable());

        menuItemRepository.save(item);
        System.out.println("✅ Added new menu item: " + item.getName());
        return "redirect:/admin/menu?created";
    }

    // ✅ Inline Edit - Update an existing item
    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @RequestParam String name,
                         @RequestParam(required = false) String description,
                         @RequestParam(required = false) Double price,
                         @RequestParam(required = false) String photoUrl,
                         @RequestParam(required = false) boolean available) {

        MenuItem item = menuItemRepository.findById(id).orElseThrow(() ->
                new IllegalArgumentException("Invalid menu item ID: " + id));

        item.setName(name);
        item.setDescription(description);
        item.setPrice(price != null ? price : 0.0);
        item.setPhotoUrl(photoUrl);
        item.setAvailable(available);

        menuItemRepository.save(item);
        System.out.println("✏️ Updated menu item ID: " + id + " (" + item.getName() + ")");
        return "redirect:/admin/menu?updated";
    }

    // ✅ Delete Menu Item
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        if (menuItemRepository.existsById(id)) {
            menuItemRepository.deleteById(id);
            System.out.println("🗑 Deleted menu item ID: " + id);
            return "redirect:/admin/menu?deleted";
        }
        System.out.println("⚠️ Attempted to delete non-existent menu item ID: " + id);
        return "redirect:/admin/menu?error";
    }
}
