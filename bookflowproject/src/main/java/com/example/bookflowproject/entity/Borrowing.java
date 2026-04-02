package com.example.bookflowproject.entity;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "borrowings")
@Getter
@Setter
@NoArgsConstructor
public class Borrowing {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "book_id", nullable = false)
    private Book book;

    @Column(name = "borrow_date", nullable = false)
    private LocalDate borrowDate;

    @Column(name = "due_date", nullable = false)
    private LocalDate dueDate;

    @Column(name = "return_date")
    private LocalDate returnDate;

    private String status; // REQUESTED, BORROWED, RETURN_REQUESTED, RETURNED, OVERDUE, REJECTED

    // --- Token & Fine System (wrapper types so existing NULL rows don't crash) ---

    @Column(name = "renewal_tokens_acquired")
    private Integer renewalTokensAcquired; // 0–3, chosen at borrow time

    @Column(name = "renewal_tokens_used")
    private Integer renewalTokensUsed;

    @Column(name = "current_period_start")
    private LocalDate currentPeriodStart; // set on librarian approval, updated on each renewal

    @Column(name = "fine_amount")
    private Double fineAmount; // committed fine in tk (locked in on renewal / clear-fine)

    @Column(name = "fine_cleared")
    private Boolean fineCleared; // true after user simulates payment

    // --- Null-safe accessors ---

    public int getEffectiveTokensAcquired() {
        return renewalTokensAcquired != null ? renewalTokensAcquired : 0;
    }

    public int getEffectiveTokensUsed() {
        return renewalTokensUsed != null ? renewalTokensUsed : 0;
    }

    public double getEffectiveFineAmount() {
        return fineAmount != null ? fineAmount : 0.0;
    }

    public boolean isEffectiveFineCleared() {
        return fineCleared != null && fineCleared;
    }

    // --- Derived helpers (not persisted) ---

    public int getRemainingTokens() {
        return getEffectiveTokensAcquired() - getEffectiveTokensUsed();
    }

    /** End of the current 7-day borrowing period. */
    public LocalDate getCurrentPeriodEnd() {
        return currentPeriodStart != null ? currentPeriodStart.plusDays(7) : dueDate;
    }

    /**
     * Real-time fine: committed fine + days overdue since current period end.
     * Returns 0 once fine has been cleared by the user.
     */
    public long calculateCurrentFine() {
        if (isEffectiveFineCleared()) return 0L;
        if ("RETURNED".equals(status) || "REJECTED".equals(status)) return (long) getEffectiveFineAmount();
        if (currentPeriodStart == null) return (long) getEffectiveFineAmount();

        LocalDate today = LocalDate.now();
        LocalDate periodEnd = getCurrentPeriodEnd();

        if (today.isAfter(periodEnd)) {
            long daysLate = ChronoUnit.DAYS.between(periodEnd, today);
            return (long) getEffectiveFineAmount() + daysLate;
        }
        return (long) getEffectiveFineAmount();
    }

    /** True when an unpaid fine is blocking a return request. */
    public boolean hasOutstandingFine() {
        return calculateCurrentFine() > 0;
    }
}