package org.example.dinedelightsystems.service;

import org.example.dine_delight.model.*;
import org.example.dine_delight.repository.*;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
public class ReportService {

    private final OrderRepository orderRepository;
    private final ReservationRepository reservationRepository;
    private final EventBookingRepository eventBookingRepository;
    private final InquiryRepository inquiryRepository;

    public ReportService(OrderRepository orderRepository,
                         ReservationRepository reservationRepository,
                         EventBookingRepository eventBookingRepository,
                         InquiryRepository inquiryRepository) {
        this.orderRepository = orderRepository;
        this.reservationRepository = reservationRepository;
        this.eventBookingRepository = eventBookingRepository;
        this.inquiryRepository = inquiryRepository;
    }

    public Map<String, Object> getMonthlySummary(int monthsBack) {
        ZoneId zone = ZoneId.systemDefault();
        DateTimeFormatter labelFmt = DateTimeFormatter.ofPattern("yyyy-MM");

        List<String> labels = new ArrayList<>();
        List<Integer> ordersCount = new ArrayList<>();
        List<Double> ordersRevenueLkr = new ArrayList<>();
        List<Integer> reservationsCount = new ArrayList<>();
        List<Double> reservationRevenueLkr = new ArrayList<>();
        List<Integer> eventsCount = new ArrayList<>();
        List<Double> eventsRevenueLkr = new ArrayList<>();
        List<Integer> inquiriesCount = new ArrayList<>();

        LocalDate startMonth = LocalDate.now().withDayOfMonth(1).minusMonths(monthsBack - 1);

        for (int i = 0; i < monthsBack; i++) {
            LocalDate monthStart = startMonth.plusMonths(i);
            LocalDate monthEnd = monthStart.plusMonths(1);
            labels.add(labelFmt.format(monthStart));

            // --- Orders ---
            int oc = 0;
            double orTotal = 0.0;
            for (Order o : orderRepository.findAll()) {
                LocalDate created = Instant.ofEpochMilli(o.getCreatedAt().toEpochMilli()).atZone(zone).toLocalDate();
                if (!created.isBefore(monthStart) && created.isBefore(monthEnd)) {
                    oc++;
                    if (o.getStatus() == OrderStatus.APPROVED || o.getStatus() == OrderStatus.DISPATCHED) {
                        orTotal += o.getTotal();
                    }
                }
            }
            ordersCount.add(oc);
            ordersRevenueLkr.add(orTotal);

            // --- Reservations ---
            int rc = 0;
            double rrTotal = 0.0;
            for (Reservation r : reservationRepository.findAll()) {
                LocalDate start = r.getStartTime().toLocalDate();
                if (!start.isBefore(monthStart) && start.isBefore(monthEnd)) {
                    rc++;
                    rrTotal += r.getTotalPrice(); // ✅ assuming now Double
                }
            }
            reservationsCount.add(rc);
            reservationRevenueLkr.add(rrTotal);

            // --- Events ---
            int ec = 0;
            double erTotal = 0.0;
            for (EventBooking b : eventBookingRepository.findAll()) {
                LocalDate start = b.getStartTime().toLocalDate();
                if (!start.isBefore(monthStart) && start.isBefore(monthEnd)) {
                    if (b.getStatus() == BookingStatus.APPROVED) {
                        ec++;
                        erTotal += b.getTotalPrice(); // ✅ assuming now Double
                    }
                }
            }
            eventsCount.add(ec);
            eventsRevenueLkr.add(erTotal);

            // --- Inquiries ---
            int ic = 0;
            for (var iq : inquiryRepository.findAll()) {
                LocalDate created = Instant.ofEpochMilli(iq.getCreatedAt().toEpochMilli()).atZone(zone).toLocalDate();
                if (!created.isBefore(monthStart) && created.isBefore(monthEnd)) {
                    ic++;
                }
            }
            inquiriesCount.add(ic);
        }

        Map<String, Object> result = new HashMap<>();
        result.put("labels", labels);
        result.put("ordersCount", ordersCount);
        result.put("ordersRevenueLkr", ordersRevenueLkr);
        result.put("reservationsCount", reservationsCount);
        result.put("reservationRevenueLkr", reservationRevenueLkr);
        result.put("eventsCount", eventsCount);
        result.put("eventsRevenueLkr", eventsRevenueLkr);
        result.put("inquiriesCount", inquiriesCount);

        // Totals for UI
        result.put("totalOrders", ordersCount.stream().mapToInt(Integer::intValue).sum());
        result.put("totalOrderRevenueLkr", ordersRevenueLkr.stream().mapToDouble(Double::doubleValue).sum());
        result.put("totalReservations", reservationsCount.stream().mapToInt(Integer::intValue).sum());
        result.put("totalReservationRevenueLkr", reservationRevenueLkr.stream().mapToDouble(Double::doubleValue).sum());
        result.put("totalEvents", eventsCount.stream().mapToInt(Integer::intValue).sum());
        result.put("totalEventRevenueLkr", eventsRevenueLkr.stream().mapToDouble(Double::doubleValue).sum());
        result.put("totalInquiries", inquiriesCount.stream().mapToInt(Integer::intValue).sum());

        return result;
    }
}

