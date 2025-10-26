package org.example.dinedelightsystems.service;

import org.example.dine_delight.model.DiningTable;
import org.example.dine_delight.model.Reservation;
import org.example.dine_delight.model.User;
import org.example.dine_delight.repository.DiningTableRepository;
import org.example.dine_delight.repository.ReservationRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class ReservationService {

    private final DiningTableRepository diningTableRepository;
    private final ReservationRepository reservationRepository;

    // Assuming a 20% advance payment rate
    private static final double ADVANCE_PAYMENT_RATE = 0.20;

    public ReservationService(DiningTableRepository diningTableRepository, ReservationRepository reservationRepository) {
        this.diningTableRepository = diningTableRepository;
        this.reservationRepository = reservationRepository;
    }

    /**
     * Helper method to calculate total price based on guest count, duration, and table rate.
     * Converts from cents (table DB field) → rupees.
     */
    private double calculateTotalPriceRupees(DiningTable table, int guestCount, int durationMinutes) {
        int hours = (int) Math.ceil(durationMinutes / 60.0);
        double pricePerPersonPerHour = table.getPricePerPersonPerHourCents() / 100.0;
        return guestCount * hours * pricePerPersonPerHour;
    }

    /** Helper to calculate advance payment */
    private double calculateAdvancePrice(double totalPrice) {
        return totalPrice * ADVANCE_PAYMENT_RATE;
    }

    /** Find available tables (for booking or editing) */
    public List<DiningTable> findAvailableTables(LocalDate date, LocalTime startTime, int durationMinutes, int guestCount, Long editId) {
        LocalDateTime start = LocalDateTime.of(date, startTime);
        LocalDateTime end = start.plusMinutes(durationMinutes);

        List<DiningTable> suitableTables = diningTableRepository.findAll().stream()
                .filter(t -> t.getCapacity() >= guestCount)
                .collect(Collectors.toList());

        return suitableTables.stream()
                .filter(t -> {
                    List<Reservation> overlaps;
                    if (editId != null) {
                        overlaps = reservationRepository.findOverlapsExcludingId(t, start, end, editId);
                    } else {
                        overlaps = reservationRepository.findOverlaps(t, start, end);
                    }
                    return overlaps.isEmpty();
                })
                .collect(Collectors.toList());
    }

    /** ✅ Book a new table with rupee-based pricing */
    @Transactional
    public Optional<Reservation> bookTable(Long tableId, User user, LocalDate date, LocalTime startTime, int durationMinutes, int guestCount) {
        DiningTable table = diningTableRepository.findWithLockingById(tableId).orElse(null);
        if (table == null || table.getCapacity() < guestCount) {
            return Optional.empty();
        }

        LocalDateTime start = LocalDateTime.of(date, startTime);
        LocalDateTime end = start.plusMinutes(durationMinutes);

        boolean hasOverlap = !reservationRepository.findOverlaps(table, start, end).isEmpty();
        if (hasOverlap) return Optional.empty();

        Reservation reservation = new Reservation();
        reservation.setDiningTable(table);
        reservation.setUser(user);
        reservation.setStartTime(start);
        reservation.setEndTime(end);
        reservation.setGuestCount(guestCount);

        // ✅ Calculate in rupees
        double totalPrice = calculateTotalPriceRupees(table, guestCount, durationMinutes);
        double advancePayment = calculateAdvancePrice(totalPrice);

        reservation.setTotalPrice(totalPrice);
        reservation.setAdvancePayment(advancePayment);
        reservation.setAdvancePaymentPaid(false);
        reservation.setAdvanceRefunded(false);

        return Optional.of(reservationRepository.save(reservation));
    }

    /** ✅ Update an existing reservation with rupee logic */
    @Transactional
    public Optional<Reservation> updateTable(Long reservationId, Long newTableId, User user, LocalDate date, LocalTime time, int durationMinutes, int guests) {
        Reservation reservation = reservationRepository.findById(reservationId).orElse(null);
        if (reservation == null || !reservation.getUser().getId().equals(user.getId())) {
            return Optional.empty();
        }

        DiningTable newTable = diningTableRepository.findWithLockingById(newTableId).orElse(null);
        if (newTable == null || newTable.getCapacity() < guests) {
            return Optional.empty();
        }

        LocalDateTime newStart = LocalDateTime.of(date, time);
        LocalDateTime newEnd = newStart.plusMinutes(durationMinutes);
        if (!reservationRepository.findOverlapsExcludingId(newTable, newStart, newEnd, reservationId).isEmpty()) {
            return Optional.empty();
        }

        reservation.setDiningTable(newTable);
        reservation.setStartTime(newStart);
        reservation.setEndTime(newEnd);
        reservation.setGuestCount(guests);

        double newTotalPrice = calculateTotalPriceRupees(newTable, guests, durationMinutes);
        double newAdvance = calculateAdvancePrice(newTotalPrice);

        // Handle payment status
        if (reservation.isAdvancePaymentPaid() && newAdvance > reservation.getAdvancePayment()) {
            reservation.setAdvancePaymentPaid(false);
        }

        reservation.setTotalPrice(newTotalPrice);
        reservation.setAdvancePayment(newAdvance);
        reservation.setAdvanceRefunded(false);

        return Optional.of(reservationRepository.save(reservation));
    }

    public List<Reservation> listUserReservations(User user) {
        return reservationRepository.findByUserOrderByStartTimeDesc(user);
    }

    public List<Reservation> getUpcomingReservations(User user) {
        return reservationRepository.findUpcomingByUser(user, LocalDateTime.now());
    }

    @Transactional
    public boolean cancelReservation(Long reservationId, User user) {
        return reservationRepository.findById(reservationId)
                .filter(r -> r.getUser().getId().equals(user.getId()))
                .map(r -> { reservationRepository.delete(r); return true; })
                .orElse(false);
    }

    @Transactional
    public boolean refundAdvance(Long reservationId, User user, String accountNumber) {
        return reservationRepository.findById(reservationId)
                .filter(r -> r.getUser().getId().equals(user.getId()))
                .filter(Reservation::isAdvancePaymentPaid)
                .filter(r -> !r.isAdvanceRefunded())
                .map(r -> {
                    r.setAdvanceRefunded(true);
                    reservationRepository.save(r);
                    return true;
                }).orElse(false);
    }
}
