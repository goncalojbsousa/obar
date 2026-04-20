package com.obar.bll.admin;

import com.obar.model.Payment;
import com.obar.model.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Read model for payment rows shown in admin financial management.
 */
public final class AdminPaymentByTripDTO {

    private final Integer paymentId;
    private final Integer tripId;
    private final String clientName;
    private final String driverName;
    private final BigDecimal amount;
    private final PaymentStatus status;
    private final LocalDateTime paymentDate;
    private final String paymentMethodType;
    private final String currencyCode;
    private final BigDecimal taxRateApplied;

    private AdminPaymentByTripDTO(
            Integer paymentId,
            Integer tripId,
            String clientName,
            String driverName,
            BigDecimal amount,
            PaymentStatus status,
            LocalDateTime paymentDate,
            String paymentMethodType,
            String currencyCode,
            BigDecimal taxRateApplied) {
        this.paymentId = paymentId;
        this.tripId = tripId;
        this.clientName = clientName;
        this.driverName = driverName;
        this.amount = amount;
        this.status = status;
        this.paymentDate = paymentDate;
        this.paymentMethodType = paymentMethodType;
        this.currencyCode = currencyCode;
        this.taxRateApplied = taxRateApplied;
    }

    public static AdminPaymentByTripDTO from(Payment payment) {
        if (payment == null) {
            throw new IllegalArgumentException("Payment must not be null.");
        }

        return new AdminPaymentByTripDTO(
                payment.getId(),
                payment.getTrip() == null ? null : payment.getTrip().getId(),
                payment.getTrip() == null || payment.getTrip().getClient() == null
                        ? null
                        : payment.getTrip().getClient().getName(),
                payment.getTrip() == null || payment.getTrip().getDriver() == null
                        ? null
                        : payment.getTrip().getDriver().getName(),
                payment.getAmount(),
                payment.getStatus(),
                payment.getPaymentDate(),
                payment.getPaymentMethod() == null ? null : payment.getPaymentMethod().getType(),
                payment.getCurrency() == null ? "EUR" : payment.getCurrency().getCode(),
                payment.getTaxRateApplied());
    }

    public Integer getPaymentId() {
        return paymentId;
    }

    public Integer getTripId() {
        return tripId;
    }

    public String getClientName() {
        return clientName;
    }

    public String getDriverName() {
        return driverName;
    }

    public BigDecimal getAmount() {
        return amount;
    }

    public PaymentStatus getStatus() {
        return status;
    }

    public LocalDateTime getPaymentDate() {
        return paymentDate;
    }

    public String getPaymentMethodType() {
        return paymentMethodType;
    }

    public String getCurrencyCode() {
        return currencyCode;
    }

    public BigDecimal getTaxRateApplied() {
        return taxRateApplied;
    }
}
