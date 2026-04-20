package com.obar.bll.admin;

import com.obar.model.enums.PaymentStatus;

/**
 * Aggregated count of payments by status.
 */
public final class AdminPaymentStatusSummaryDTO {

    private final PaymentStatus status;
    private final long total;

    public AdminPaymentStatusSummaryDTO(PaymentStatus status, long total) {
        this.status = status;
        this.total = total;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public long getTotal() {
        return total;
    }
}
