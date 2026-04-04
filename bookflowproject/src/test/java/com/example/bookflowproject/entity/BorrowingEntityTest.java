package com.example.bookflowproject.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

class BorrowingEntityTest {

    @Nested
    @DisplayName("getRemainingTokens")
    class GetRemainingTokens {

        @Test
        void shouldReturnZeroWhenBothAcquiredAndUsedAreNull() {
            Borrowing b = new Borrowing();
            assertEquals(0, b.getRemainingTokens());
        }

        @Test
        void shouldReturnAcquiredMinusUsed() {
            Borrowing b = new Borrowing();
            b.setRenewalTokensAcquired(3);
            b.setRenewalTokensUsed(1);
            assertEquals(2, b.getRemainingTokens());
        }

        @Test
        void shouldTreatNullUsedAsZero() {
            Borrowing b = new Borrowing();
            b.setRenewalTokensAcquired(2);
            assertEquals(2, b.getRemainingTokens());
        }
    }

    @Nested
    @DisplayName("getCurrentPeriodEnd")
    class GetCurrentPeriodEnd {

        @Test
        void shouldReturnDueDateWhenCurrentPeriodStartIsNull() {
            Borrowing b = new Borrowing();
            LocalDate dueDate = LocalDate.of(2026, 5, 1);
            b.setDueDate(dueDate);
            assertEquals(dueDate, b.getCurrentPeriodEnd());
        }

        @Test
        void shouldReturn7DaysAfterCurrentPeriodStart() {
            Borrowing b = new Borrowing();
            LocalDate periodStart = LocalDate.of(2026, 3, 1);
            b.setCurrentPeriodStart(periodStart);
            b.setDueDate(LocalDate.of(2026, 3, 20)); // should be ignored when periodStart is set
            assertEquals(LocalDate.of(2026, 3, 8), b.getCurrentPeriodEnd());
        }
    }

    @Nested
    @DisplayName("calculateCurrentFine")
    class CalculateCurrentFine {

        @Test
        void shouldReturnZeroWhenFineIsCleared() {
            Borrowing b = new Borrowing();
            b.setStatus("BORROWED");
            b.setCurrentPeriodStart(LocalDate.now().minusDays(10)); // 3 days overdue
            b.setFineAmount(5.0);
            b.setFineCleared(true);
            assertEquals(0L, b.calculateCurrentFine());
        }

        @Test
        void shouldReturnLockedFineAmountForReturnedStatus() {
            Borrowing b = new Borrowing();
            b.setStatus("RETURNED");
            b.setFineAmount(3.0);
            assertEquals(3L, b.calculateCurrentFine());
        }

        @Test
        void shouldReturnLockedFineAmountForRejectedStatus() {
            Borrowing b = new Borrowing();
            b.setStatus("REJECTED");
            b.setFineAmount(2.0);
            assertEquals(2L, b.calculateCurrentFine());
        }

        @Test
        void shouldReturnStoredFineWhenCurrentPeriodStartIsNull() {
            Borrowing b = new Borrowing();
            b.setStatus("BORROWED");
            b.setFineAmount(4.0);
            // currentPeriodStart null → early return with committed fineAmount
            assertEquals(4L, b.calculateCurrentFine());
        }

        @Test
        void shouldReturnZeroFineWhenWithinCurrentPeriod() {
            Borrowing b = new Borrowing();
            b.setStatus("BORROWED");
            b.setCurrentPeriodStart(LocalDate.now().minusDays(3)); // period ends in 4 days
            b.setFineAmount(0.0);
            assertEquals(0L, b.calculateCurrentFine());
        }

        @Test
        void shouldAccrue1TkPerDayPastPeriodEnd() {
            Borrowing b = new Borrowing();
            b.setStatus("BORROWED");
            // period started 10 days ago → ended 3 days ago → 3 days late
            b.setCurrentPeriodStart(LocalDate.now().minusDays(10));
            b.setFineAmount(0.0);
            assertEquals(3L, b.calculateCurrentFine());
        }

        @Test
        void shouldAddAccruedDaysToExistingCommittedFine() {
            Borrowing b = new Borrowing();
            b.setStatus("OVERDUE");
            // period started 10 days ago → ended 3 days ago → 3 days late
            b.setCurrentPeriodStart(LocalDate.now().minusDays(10));
            b.setFineAmount(5.0); // 5 tk committed from a prior renewal
            // total = 5 + 3 = 8
            assertEquals(8L, b.calculateCurrentFine());
        }
    }

    @Nested
    @DisplayName("hasOutstandingFine")
    class HasOutstandingFine {

        @Test
        void shouldReturnTrueWhenPastPeriodEndAndFinePositive() {
            Borrowing b = new Borrowing();
            b.setStatus("BORROWED");
            b.setCurrentPeriodStart(LocalDate.now().minusDays(10)); // 3 days overdue
            b.setFineAmount(0.0);
            assertTrue(b.hasOutstandingFine());
        }

        @Test
        void shouldReturnFalseWhenWithinCurrentPeriod() {
            Borrowing b = new Borrowing();
            b.setStatus("BORROWED");
            b.setCurrentPeriodStart(LocalDate.now().minusDays(3)); // 4 days remaining
            b.setFineAmount(0.0);
            assertFalse(b.hasOutstandingFine());
        }

        @Test
        void shouldReturnFalseWhenFineHasBeenCleared() {
            Borrowing b = new Borrowing();
            b.setStatus("OVERDUE");
            b.setCurrentPeriodStart(LocalDate.now().minusDays(10));
            b.setFineAmount(3.0);
            b.setFineCleared(true);
            assertFalse(b.hasOutstandingFine());
        }

        @Test
        void shouldReturnFalseWhenNoCommittedFineAndNoPeriodStart() {
            Borrowing b = new Borrowing();
            b.setStatus("BORROWED");
            // currentPeriodStart null → calculateCurrentFine returns fineAmount = 0
            assertFalse(b.hasOutstandingFine());
        }
    }
}
