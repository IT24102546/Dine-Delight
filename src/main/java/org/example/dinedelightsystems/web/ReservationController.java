package org.example.dinedelightsystems.web;

import org.example.dine_delight.model.DiningTable;
import org.example.dine_delight.model.User;
import org.example.dine_delight.repository.UserRepository;
import org.example.dine_delight.service.ReservationService;
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
@RequestMapping("/reservations")
public class ReservationController {

    private final ReservationService reservationService;
    private final UserRepository userRepository;

    public ReservationController(ReservationService reservationService, UserRepository userRepository) {
        this.reservationService = reservationService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public String reservationHome() {
        return "reservations/index";
    }

    @GetMapping("/search")
    public String searchAvailability(@RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                                     @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime time,
                                     @RequestParam int durationMinutes,
                                     @RequestParam int guests,
                                     @RequestParam(required = false) Long editId, // Added for edit flow
                                     Model model) {
        // Pass editId (even if null) to the service
        List<DiningTable> available = reservationService.findAvailableTables(date, time, durationMinutes, guests, editId);

        model.addAttribute("availableTables", available);
        model.addAttribute("date", date);
        model.addAttribute("time", time);
        model.addAttribute("durationMinutes", durationMinutes);
        model.addAttribute("guests", guests);
        model.addAttribute("editId", editId); // Pass editId to the view
        return "reservations/index";
    }

    @PostMapping("/book")
    public String book(@RequestParam Long tableId,
                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                       @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime time,
                       @RequestParam int durationMinutes,
                       @RequestParam int guests,
                       @AuthenticationPrincipal UserDetails principal,
                       Model model) {
        User user = userRepository.findByEmail(principal.getUsername()).orElseThrow();
        return reservationService.bookTable(tableId, user, date, time, durationMinutes, guests)
                .map(r -> "redirect:/reservations/my?success")
                .orElse("redirect:/reservations?conflict");
    }

    @PostMapping("/update") // New method to handle updates
    public String update(@RequestParam Long tableId,
                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
                         @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.TIME) LocalTime time,
                         @RequestParam int durationMinutes,
                         @RequestParam int guests,
                         @RequestParam Long editId, // The ID of the reservation to update
                         @AuthenticationPrincipal UserDetails principal) {
        User user = userRepository.findByEmail(principal.getUsername()).orElseThrow();

        return reservationService.updateTable(editId, tableId, user, date, time, durationMinutes, guests)
                .map(r -> "redirect:/reservations/my?updated")
                .orElse("redirect:/reservations/search?conflict&date=" + date + "&time=" + time + "&durationMinutes=" + durationMinutes + "&guests=" + guests + "&editId=" + editId);
    }

    @GetMapping("/my")
    public String myReservations(@AuthenticationPrincipal UserDetails principal, Model model) {
        User user = userRepository.findByEmail(principal.getUsername()).orElseThrow();
        model.addAttribute("reservations", reservationService.listUserReservations(user));
        return "reservations/my";
    }

    @PostMapping("/cancel")
    public String cancel(@RequestParam Long id, @AuthenticationPrincipal UserDetails principal) {
        User user = userRepository.findByEmail(principal.getUsername()).orElseThrow();
        boolean ok = reservationService.cancelReservation(id, user);
        return ok ? "redirect:/reservations/my?cancelled" : "redirect:/reservations/my?error";
    }

    @PostMapping("/refund")
    public String refund(@RequestParam Long id,
                         @RequestParam String accountNumber,
                         @AuthenticationPrincipal UserDetails principal) {
        User user = userRepository.findByEmail(principal.getUsername()).orElseThrow();
        boolean ok = reservationService.refundAdvance(id, user, accountNumber);
        return ok ? "redirect:/reservations/my?refunded" : "redirect:/reservations/my?error";
    }
}
