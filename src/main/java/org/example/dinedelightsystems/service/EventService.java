package org.example.dinedelightsystems.service;

import org.example.dine_delight.model.*;
import org.example.dine_delight.repository.EventBookingRepository;
import org.example.dine_delight.repository.EventSpaceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class EventService {

    private final EventSpaceRepository eventSpaceRepository;
    private final EventBookingRepository eventBookingRepository;

    public EventService(EventSpaceRepository eventSpaceRepository, EventBookingRepository eventBookingRepository) {
        this.eventSpaceRepository = eventSpaceRepository;
        this.eventBookingRepository = eventBookingRepository;
    }

    // 🔹 Find available event spaces
    public List<EventSpace> findAvailableSpaces(LocalDate date, LocalTime startTime, int durationMinutes, int guestCount) {
        LocalDateTime start = LocalDateTime.of(date, startTime);
        LocalDateTime end = start.plusMinutes(durationMinutes);
        return eventSpaceRepository.findAll().stream()
                .filter(s -> s.getCapacity() >= guestCount)
                .filter(s -> {
                    List<EventBooking> overlaps = eventBookingRepository.findOverlaps(s, start, end);
                    return overlaps == null || overlaps.isEmpty();
                })
                .collect(Collectors.toList());
    }

    // 🔹 Create new booking request
    @Transactional
    public Optional<EventBooking> createRequest(Long spaceId, User user, LocalDate date, LocalTime startTime,
                                                int durationMinutes, int guestCount, String services) {
        EventSpace space = eventSpaceRepository.findById(spaceId).orElse(null);
        if (space == null || space.getCapacity() < guestCount) {
            return Optional.empty();
        }

        LocalDateTime start = LocalDateTime.of(date, startTime);
        LocalDateTime end = start.plusMinutes(durationMinutes);

        // Space availability check
        List<EventBooking> spaceOverlaps = eventBookingRepository.findOverlaps(space, start, end);
        if (spaceOverlaps != null && !spaceOverlaps.isEmpty()) {
            return Optional.empty();
        }

        // Location availability check
        List<EventBooking> locationOverlaps =
                eventBookingRepository.findApprovedOverlapsAtLocation(space.getLocation(), start, end);
        if (locationOverlaps != null && !locationOverlaps.isEmpty()) {
            return Optional.empty();
        }

        // ✅ Create booking (amounts in rupees)
        EventBooking booking = new EventBooking();
        booking.setEventSpace(space);
        booking.setUser(user);
        booking.setStartTime(start);
        booking.setEndTime(end);
        booking.setGuestCount(guestCount);
        booking.setServices(services);
        booking.setStatus(BookingStatus.PENDING);

        int hours = (int) Math.ceil(durationMinutes / 60.0);
        double pricePerPersonPerHour = space.getPricePerPersonPerHourCents() / 100.0;
        double totalPrice = guestCount * hours * pricePerPersonPerHour;
        double advancePayment = totalPrice * 0.20;

        booking.setTotalPrice(totalPrice);
        booking.setAdvancePayment(advancePayment);
        booking.setAdvancePaymentPaid(false);

        return Optional.of(eventBookingRepository.save(booking));
    }

    // 🔹 Get all bookings for a user
    public List<EventBooking> userBookings(User user) {
        return eventBookingRepository.findByUserOrderByStartTimeDesc(user);
    }

    // 🔹 Get upcoming bookings (for dashboard)
    public List<EventBooking> getUpcomingEventBookings(User user) {
        return eventBookingRepository.findUpcomingByUser(user, LocalDateTime.now());
    }

    // 🔹 Get only paid pending bookings (for admin)
    public List<EventBooking> pendingBookings() {
        return eventBookingRepository.findPaidPendingBookings();
    }

    // 🔹 Approve a booking
    @Transactional
    public boolean approve(Long bookingId) {
        return eventBookingRepository.findById(bookingId)
                .map(b -> {
                    // Conflict check
                    List<EventBooking> overlaps = eventBookingRepository.findOverlaps(b.getEventSpace(), b.getStartTime(), b.getEndTime());
                    boolean clashes = overlaps != null && overlaps.stream()
                            .anyMatch(other -> !other.getId().equals(b.getId()) && other.getStatus() == BookingStatus.APPROVED);
                    if (clashes) return false;

                    List<EventBooking> locationOverlaps =
                            eventBookingRepository.findApprovedOverlapsAtLocation(b.getEventSpace().getLocation(), b.getStartTime(), b.getEndTime());
                    boolean locationClash = locationOverlaps != null && locationOverlaps.stream()
                            .anyMatch(other -> !other.getId().equals(b.getId()));
                    if (locationClash) return false;

                    b.setStatus(BookingStatus.APPROVED);
                    eventBookingRepository.save(b);
                    return true;
                }).orElse(false);
    }

    // 🔹 Reject a booking
    @Transactional
    public boolean reject(Long bookingId) {
        return eventBookingRepository.findById(bookingId)
                .map(b -> {
                    b.setStatus(BookingStatus.REJECTED);
                    eventBookingRepository.save(b);
                    return true;
                }).orElse(false);
    }

    // 🔹 Find booking by ID & user
    public EventBooking findByIdAndUser(Long id, User user) {
        return eventBookingRepository.findById(id)
                .filter(b -> b.getUser().getId().equals(user.getId()))
                .orElse(null);
    }

    // 🔹 Update user booking
    @Transactional
    public boolean updateBooking(Long id, User user, LocalDate date, LocalTime time,
                                 int durationMinutes, int guests, String services) {
        return eventBookingRepository.findById(id).map(b -> {
            if (!b.getUser().getId().equals(user.getId())) return false;

            LocalDateTime start = LocalDateTime.of(date, time);
            LocalDateTime end = start.plusMinutes(durationMinutes);

            b.setStartTime(start);
            b.setEndTime(end);
            b.setGuestCount(guests);
            b.setServices(services);

            int hours = (int) Math.ceil(durationMinutes / 60.0);
            double pricePerPersonPerHour = b.getEventSpace().getPricePerPersonPerHourCents() / 100.0;
            double totalPrice = guests * hours * pricePerPersonPerHour;
            double advancePayment = totalPrice * 0.20;

            b.setTotalPrice(totalPrice);
            b.setAdvancePayment(advancePayment);

            eventBookingRepository.save(b);
            return true;
        }).orElse(false);
    }

    // 🔹 Delete a booking (only if unpaid)
    @Transactional
    public boolean deleteBooking(Long bookingId, User user) {
        return eventBookingRepository.findById(bookingId)
                .filter(b -> b.getUser().getId().equals(user.getId()))
                .filter(b -> !b.isAdvancePaymentPaid())
                .map(b -> {
                    eventBookingRepository.delete(b);
                    return true;
                }).orElse(false);
    }

    // 🔹 Refund advance
    @Transactional
    public boolean refundAdvance(Long bookingId, User user, String accountNumber) {
        return eventBookingRepository.findById(bookingId)
                .filter(b -> b.getUser().getId().equals(user.getId()))
                .filter(EventBooking::isAdvancePaymentPaid)
                .filter(b -> !b.isAdvanceRefunded())
                .map(b -> {
                    b.setRefundAccountNumber(accountNumber);
                    b.setAdvanceRefunded(true);
                    eventBookingRepository.save(b);
                    return true;
                }).orElse(false);
    }
}

