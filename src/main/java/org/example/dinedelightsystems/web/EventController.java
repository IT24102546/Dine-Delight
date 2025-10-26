package org.example.dinedelightsystems.web;

import org.example.dine_delight.model.EventSpace;
import org.example.dine_delight.model.User;
import org.example.dine_delight.repository.UserRepository;
import org.example.dine_delight.service.EventService;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Controller
@RequestMapping("/events")
public class EventController {

    private final EventService eventService;
    private final UserRepository userRepository;

    public EventController(EventService eventService, UserRepository userRepository) {
        this.eventService = eventService;
        this.userRepository = userRepository;
    }

    // 🔹 Event home page
    @GetMapping
    public String eventHome(Model model) {
        model.addAttribute("date", LocalDate.now());
        model.addAttribute("time", LocalTime.of(18, 0));
        model.addAttribute("durationMinutes", 180);
        model.addAttribute("guests", 20);
        model.addAttribute("availableSpaces", null);
        return "events/index";
    }

    // 🔹 Search available event spaces
    @GetMapping("/search")
    public String search(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime time,
                         @RequestParam int durationMinutes,
                         @RequestParam int guests,
                         Model model) {
        List<EventSpace> spaces = eventService.findAvailableSpaces(date, time, durationMinutes, guests);
        model.addAttribute("availableSpaces", spaces);
        model.addAttribute("date", date);
        model.addAttribute("time", time);
        model.addAttribute("durationMinutes", durationMinutes);
        model.addAttribute("guests", guests);
        return "events/index";
    }

    // 🔹 Submit new booking request
    @PostMapping("/request")
    public String request(@RequestParam Long spaceId,
                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                          @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime time,
                          @RequestParam int durationMinutes,
                          @RequestParam int guests,
                          @RequestParam(required = false) String services,
                          @AuthenticationPrincipal UserDetails principal) {
        User user = userRepository.findByEmail(principal.getUsername()).orElseThrow();
        return eventService.createRequest(spaceId, user, date, time, durationMinutes, guests, services)
                .map(b -> "redirect:/events/my?submitted")
                .orElse("redirect:/events?conflict");
    }

    // 🔹 Display all bookings for the logged-in user
    @GetMapping("/my")
    public String my(@AuthenticationPrincipal UserDetails principal, Model model) {
        User user = userRepository.findByEmail(principal.getUsername()).orElseThrow();
        model.addAttribute("bookings", eventService.userBookings(user));
        return "events/my";
    }

    // 🔹 Edit form page (loads edit.html)
    @GetMapping("/edit/{id}")
    public String edit(@PathVariable Long id,
                       @AuthenticationPrincipal UserDetails principal,
                       Model model) {
        User user = userRepository.findByEmail(principal.getUsername()).orElseThrow();
        var booking = eventService.findByIdAndUser(id, user);

        model.addAttribute("booking", booking);
        return "events/edit"; // loads templates/events/edit.html
    }

    // 🔹 Handle edit form submission
    @PostMapping("/update")
    public String update(@RequestParam Long id,
                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime time,
                         @RequestParam int durationMinutes,
                         @RequestParam int guests,
                         @RequestParam(required = false) String services,
                         @AuthenticationPrincipal UserDetails principal) {
        User user = userRepository.findByEmail(principal.getUsername()).orElseThrow();
        boolean ok = eventService.updateBooking(id, user, date, time, durationMinutes, guests, services);
        return ok ? "redirect:/events/my?updated" : "redirect:/events/my?error";
    }

    // 🔹 Delete a booking
    @PostMapping("/delete")
    public String delete(@RequestParam Long id, @AuthenticationPrincipal UserDetails principal) {
        User user = userRepository.findByEmail(principal.getUsername()).orElseThrow();
        boolean ok = eventService.deleteBooking(id, user);
        return ok ? "redirect:/events/my?deleted" : "redirect:/events/my?error";
    }

    // 🔹 Refund advance payment
    @PostMapping("/refund")
    public String refund(@RequestParam Long id,
                         @RequestParam String accountNumber,
                         @AuthenticationPrincipal UserDetails principal) {
        User user = userRepository.findByEmail(principal.getUsername()).orElseThrow();
        boolean ok = eventService.refundAdvance(id, user, accountNumber);
        return ok ? "redirect:/events/my?refunded" : "redirect:/events/my?error";
    }
}

