package com.obar.bll.admin;

import com.obar.model.Payment;
import com.obar.model.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record AdminPaymentByTripDTO(
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

        public static AdminPaymentByTripDTO from(Payment payment) {
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
}
