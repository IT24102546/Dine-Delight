package org.example.dinedelightsystems.web;

import org.example.dine_delight.model.*;
import org.example.dine_delight.repository.MenuItemRepository;
import org.example.dine_delight.repository.OrderRepository;
import org.example.dine_delight.repository.UserRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpSession;
import java.util.HashMap;
import java.util.Map;

@Controller
@RequestMapping("/checkout")
public class CheckoutController {

    private final MenuItemRepository menuItemRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    public CheckoutController(MenuItemRepository menuItemRepository,
                              OrderRepository orderRepository,
                              UserRepository userRepository) {
        this.menuItemRepository = menuItemRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
    }

    // ✅ Add to session cart
    @PostMapping("/buy")
    public String buyNow(@RequestParam Long id,
                         @RequestParam(defaultValue = "1") Integer qty,
                         HttpSession session) {
        Map<Long, Integer> cart = new HashMap<>();
        cart.put(id, Math.max(1, qty));
        session.setAttribute("CART", cart);
        return "redirect:/checkout";
    }

    // ✅ Show checkout page
    @GetMapping
    public String review(Model model, HttpSession session) {
        @SuppressWarnings("unchecked")
        Map<Long, Integer> cart = (Map<Long, Integer>) session.getAttribute("CART");
        if (cart == null || cart.isEmpty()) return "redirect:/menu";

        double subtotal = 0.0;
        Map<MenuItem, Integer> lines = new HashMap<>();

        for (var e : cart.entrySet()) {
            var item = menuItemRepository.findById(e.getKey()).orElseThrow();
            int qty = e.getValue();
            lines.put(item, qty);
            subtotal += item.getPrice() * qty; // ✅ fixed
        }

        model.addAttribute("lines", lines);
        model.addAttribute("subtotal", subtotal);
        return "checkout/index";
    }

    // ✅ Handle payment
    @PostMapping
    public String pay(@RequestParam String cardNumber,
                      @RequestParam String name,
                      @RequestParam String expiry,
                      @RequestParam String cvv,
                      @AuthenticationPrincipal UserDetails principal,
                      HttpSession session) {
        if (!cardNumber.matches("\\d{16}") || !cvv.matches("\\d{3}")) {
            return "redirect:/checkout?invalid";
        }

        @SuppressWarnings("unchecked")
        Map<Long, Integer> cart = (Map<Long, Integer>) session.getAttribute("CART");
        if (cart == null || cart.isEmpty()) return "redirect:/menu";

        User user = userRepository.findByEmail(principal.getUsername()).orElseThrow();
        Order order = new Order();
        order.setUser(user);

        double total = 0.0;

        for (var e : cart.entrySet()) {
            var item = menuItemRepository.findById(e.getKey()).orElseThrow();
            if (!item.isAvailable()) continue;

            OrderItem oi = new OrderItem();
            oi.setOrder(order);
            oi.setMenuItem(item);

            int qty = Math.max(1, e.getValue());
            oi.setQuantity(qty);

            // ✅ Use Double-based calculations
            double lineTotal = item.getPrice() * qty;
            oi.setLineTotal(lineTotal);

            order.getItems().add(oi);
            total += lineTotal;
        }

        order.setTotal(total);
        order.setStatus(OrderStatus.PENDING);
        orderRepository.save(order);

        session.removeAttribute("CART");
        return "redirect:/orders?placed";
    }
}

