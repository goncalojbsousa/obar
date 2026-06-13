package com.obar.bll.admin;

import java.math.BigDecimal;

public record AdminFinancialOverviewDTO(
                BigDecimal totalIncome,
                BigDecimal periodIncome,
                long totalPayments,
                long pendingPayments,
                long processedPayments,
                long failedPayments,
                long refundedPayments) {
}
