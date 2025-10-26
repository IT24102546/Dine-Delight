package org.example.dinedelightsystems.model;

import jakarta.persistence.*;
import java.time.Duration;
import java.time.LocalDateTime;

@Entity
@Table(name = "reservation")
public class Reservation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "table_id")
    private DiningTable diningTable;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(nullable = false)
    private LocalDateTime startTime;

    @Column(nullable = false)
    private LocalDateTime endTime;

    @Column(nullable = false)
    private int guestCount;

    // ✅ Changed from cents → rupees
    @Column(nullable = false)
    private Double totalPrice;

    @Column(nullable = false)
    private Double advancePayment;

    @Column(nullable = false)
    private boolean advancePaymentPaid = false;

    @Column(nullable = false)
    private boolean advanceRefunded = false;

    @Column(length = 64)
    private String refundAccountNumber;

    // ---------- Getters & Setters ----------

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public DiningTable getDiningTable() { return diningTable; }
    public void setDiningTable(DiningTable diningTable) { this.diningTable = diningTable; }

    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public LocalDateTime getEndTime() { return endTime; }
    public void setEndTime(LocalDateTime endTime) { this.endTime = endTime; }

    public int getGuestCount() { return guestCount; }
    public void setGuestCount(int guestCount) { this.guestCount = guestCount; }

    public Double getTotalPrice() { return totalPrice; }
    public void setTotalPrice(Double totalPrice) { this.totalPrice = totalPrice; }

    public Double getAdvancePayment() { return advancePayment; }
    public void setAdvancePayment(Double advancePayment) { this.advancePayment = advancePayment; }

    public boolean isAdvancePaymentPaid() { return advancePaymentPaid; }
    public void setAdvancePaymentPaid(boolean advancePaymentPaid) { this.advancePaymentPaid = advancePaymentPaid; }

    public boolean isAdvanceRefunded() { return advanceRefunded; }
    public void setAdvanceRefunded(boolean advanceRefunded) { this.advanceRefunded = advanceRefunded; }

    public String getRefundAccountNumber() { return refundAccountNumber; }
    public void setRefundAccountNumber(String refundAccountNumber) { this.refundAccountNumber = refundAccountNumber; }

    // ---------- NEW Helper for Thymeleaf ----------
    @Transient
    public long getDurationMinutes() {
        if (startTime != null && endTime != null) {
            return Duration.between(startTime, endTime).toMinutes();
        }
        return 0;
    }
}

