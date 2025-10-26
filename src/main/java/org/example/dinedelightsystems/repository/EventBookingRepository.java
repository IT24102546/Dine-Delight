package org.example.dinedelightsystems.repository;

import org.example.dine_delight.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface EventBookingRepository extends JpaRepository<EventBooking, Long> {

    // 🔹 Detect overlapping bookings in same space
    @Query("select b from EventBooking b where b.eventSpace = :space and b.status <> 'REJECTED' and b.endTime > :start and b.startTime < :end")
    List<EventBooking> findOverlaps(@Param("space") EventSpace space,
                                    @Param("start") LocalDateTime start,
                                    @Param("end") LocalDateTime end);

    // 🔹 Check approved overlaps at same location
    @Query("select b from EventBooking b where b.eventSpace.location = :location and b.status = 'APPROVED' and b.endTime > :start and b.startTime < :end")
    List<EventBooking> findApprovedOverlapsAtLocation(@Param("location") EventLocation location,
                                                      @Param("start") LocalDateTime start,
                                                      @Param("end") LocalDateTime end);

    // 🔹 All bookings for a user
    List<EventBooking> findByUserOrderByStartTimeDesc(User user);

    // 🔹 Pending bookings ordered by start time
    List<EventBooking> findByStatusOrderByStartTimeAsc(BookingStatus status);

    // 🔹 Paid + Pending bookings (for Admin)
    @Query("select b from EventBooking b where b.status = 'PENDING' and b.advancePaymentPaid = true order by b.startTime asc")
    List<EventBooking> findPaidPendingBookings();

    // 🔹 Upcoming approved bookings for a user
    @Query("select b from EventBooking b where b.user = :user and b.startTime > :now and b.status = 'APPROVED' order by b.startTime asc")
    List<EventBooking> findUpcomingByUser(@Param("user") User user, @Param("now") LocalDateTime now);
}
