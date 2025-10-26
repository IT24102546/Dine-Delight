package org.example.dinedelightsystems.web;

import org.example.dine_delight.model.*;
import org.example.dine_delight.repository.MenuItemRepository;
import org.example.dine_delight.repository.OrderRepository;
import org.example.dine_delight.repository.UserRepository;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/preorder")
public class PreOrderController {

    private final MenuItemRepository menuItemRepository;
    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    public PreOrderController(MenuItemRepository menuItemRepository, OrderRepository orderRepository, UserRepository userRepository) {
        this.menuItemRepository = menuItemRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("/add")
    public String addPreOrder(@RequestParam Long id,
                              @RequestParam(defaultValue = "1") Integer qty,
                              @AuthenticationPrincipal UserDetails principal) {
        var item = menuItemRepository.findById(id).orElseThrow();

        if (!item.isAvailable()) {
            return "redirect:/menu?unavailable";
        }

        User user = userRepository.findByEmail(principal.getUsername()).orElseThrow();

        // Create a new order with PREORDER status
        Order order = new Order();
        order.setUser(user);
        order.setStatus(OrderStatus.PREORDER); // Pre-orders start as PREORDER

        // Add the menu item to the order
        OrderItem orderItem = new OrderItem();
        orderItem.setOrder(order);
        orderItem.setMenuItem(item);
        orderItem.setQuantity(Math.max(1, qty));

        // ✅ Use item.getPrice() since it's now in rupees
        double lineTotal = item.getPrice() * orderItem.getQuantity();
        orderItem.setLineTotal(lineTotal); // change your model to use 'lineTotal' (Double)

        // ✅ Set totals in rupees too
        order.setTotal(lineTotal);

        // Add the order item to the order list
        order.getItems().add(orderItem);

        orderRepository.save(order);

        return "redirect:/orders?preordered";
    }
}
