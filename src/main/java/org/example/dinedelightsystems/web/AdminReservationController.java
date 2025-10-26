package org.example.dinedelightsystems.web;

import org.example.dine_delight.model.Reservation;
import org.example.dine_delight.repository.ReservationRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/reservations")
public class AdminReservationController {

    private final ReservationRepository reservationRepository;

    public AdminReservationController(ReservationRepository reservationRepository) {
        this.reservationRepository = reservationRepository;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("reservations", reservationRepository.findAll());
        return "admin/reservations";
    }

    @PostMapping("/{id}/approve")
    public String approve(@PathVariable Long id) {
        Reservation reservation = reservationRepository.findById(id).orElseThrow();
        reservation.setAdvancePaymentPaid(true);
        reservationRepository.save(reservation);
        return "redirect:/admin/reservations?approved";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        if (reservationRepository.existsById(id)) {
            reservationRepository.deleteById(id);
            return "redirect:/admin/reservations?deleted";
        }
        return "redirect:/admin/reservations?error";
    }
}



