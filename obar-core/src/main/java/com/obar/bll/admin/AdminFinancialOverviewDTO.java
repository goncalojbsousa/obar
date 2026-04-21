package com.obar.bll.admin;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;

/**
 * Aggregated values used on the admin financial dashboard.
 */
public final class AdminFinancialOverviewDTO {

    private final BigDecimal totalIncome;
    private final BigDecimal periodIncome;
    private final long totalPayments;
    private final long pendingPayments;
    private final long processedPayments;
    private final long failedPayments;
    private final long refundedPayments;
    private final List<AdminPaymentStatusSummaryDTO> paymentStatuses;

    public AdminFinancialOverviewDTO(
            BigDecimal totalIncome,
            BigDecimal periodIncome,
            long totalPayments,
            long pendingPayments,
            long processedPayments,
            long failedPayments,
            long refundedPayments,
            List<AdminPaymentStatusSummaryDTO> paymentStatuses) {
        this.totalIncome = totalIncome;
        this.periodIncome = periodIncome;
        this.totalPayments = totalPayments;
        this.pendingPayments = pendingPayments;
        this.processedPayments = processedPayments;
        this.failedPayments = failedPayments;
        this.refundedPayments = refundedPayments;
        this.paymentStatuses = paymentStatuses == null
                ? List.of()
                : Collections.unmodifiableList(paymentStatuses);
    }

    public BigDecimal getTotalIncome() {
        return totalIncome;
    }

    public BigDecimal getPeriodIncome() {
        return periodIncome;
    }

    public long getTotalPayments() {
        return totalPayments;
    }

    public long getPendingPayments() {
        return pendingPayments;
    }

    public long getProcessedPayments() {
        return processedPayments;
    }

    public long getFailedPayments() {
        return failedPayments;
    }

    public long getRefundedPayments() {
        return refundedPayments;
    }

    public List<AdminPaymentStatusSummaryDTO> getPaymentStatuses() {
        return paymentStatuses;
    }
}
