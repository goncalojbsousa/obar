package com.obar.bll.admin;

import com.obar.dal.PaymentRepository;
import com.obar.dal.TaxRateRepository;
import com.obar.model.TaxRate;
import com.obar.model.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/**
 * Admin BLL use cases for financial dashboard and tax-rate management.
 */
final class AdminFinancialService {

    private final PaymentRepository paymentRepository;
    private final TaxRateRepository taxRateRepository;

    AdminFinancialService(PaymentRepository paymentRepository, TaxRateRepository taxRateRepository) {
        if (paymentRepository == null) {
            throw new IllegalArgumentException("PaymentRepository must not be null.");
        }
        if (taxRateRepository == null) {
            throw new IllegalArgumentException("TaxRateRepository must not be null.");
        }
        this.paymentRepository = paymentRepository;
        this.taxRateRepository = taxRateRepository;
    }

    AdminFinancialOverviewDTO getFinancialOverview(AdminFinancialPeriod period) {
        AdminFinancialPeriod resolvedPeriod = resolvePeriod(period);
        LocalDateTime periodStart = resolvePeriodStart(resolvedPeriod);
        LocalDateTime periodEnd = resolvePeriodEnd(resolvedPeriod);

        BigDecimal totalIncome = paymentRepository.sumAmountByStatus(PaymentStatus.PROCESSED, null, null);
        BigDecimal periodIncome = paymentRepository.sumAmountByStatus(PaymentStatus.PROCESSED, periodStart, periodEnd);
        Map<PaymentStatus, Long> paymentCountsByStatus = countPaymentsByStatus(periodStart, periodEnd);

        List<AdminPaymentStatusSummaryDTO> statusSummaries = List.of(
                new AdminPaymentStatusSummaryDTO(PaymentStatus.PENDING,
                        paymentCountsByStatus.get(PaymentStatus.PENDING)),
                new AdminPaymentStatusSummaryDTO(PaymentStatus.PROCESSED,
                        paymentCountsByStatus.get(PaymentStatus.PROCESSED)),
                new AdminPaymentStatusSummaryDTO(PaymentStatus.FAILED, paymentCountsByStatus.get(PaymentStatus.FAILED)),
                new AdminPaymentStatusSummaryDTO(PaymentStatus.REFUNDED,
                        paymentCountsByStatus.get(PaymentStatus.REFUNDED)));

        long totalPayments = statusSummaries.stream().mapToLong(AdminPaymentStatusSummaryDTO::getTotal).sum();
        return new AdminFinancialOverviewDTO(
                totalIncome,
                periodIncome,
                totalPayments,
                paymentCountsByStatus.get(PaymentStatus.PENDING),
                paymentCountsByStatus.get(PaymentStatus.PROCESSED),
                paymentCountsByStatus.get(PaymentStatus.FAILED),
                paymentCountsByStatus.get(PaymentStatus.REFUNDED),
                statusSummaries);
    }

    List<AdminPaymentByTripDTO> listPaymentsByTrip(AdminFinancialPeriod period) {
        AdminFinancialPeriod resolvedPeriod = resolvePeriod(period);
        LocalDateTime periodStart = resolvePeriodStart(resolvedPeriod);
        LocalDateTime periodEnd = resolvePeriodEnd(resolvedPeriod);

        return paymentRepository.findAllForAdmin(periodStart, periodEnd).stream()
                .map(AdminPaymentByTripDTO::from)
                .toList();
    }

    List<AdminTaxRateDTO> listTaxRates() {
        return taxRateRepository.findAllOrderedForAdmin().stream()
                .map(AdminTaxRateDTO::from)
                .toList();
    }

    AdminTaxRateDTO updateTaxRate(Integer taxRateId, AdminTaxRateCommand command) {
        validateTaxRateCommand(taxRateId, command);

        TaxRate taxRate = taxRateRepository.findById(taxRateId)
                .orElseThrow(() -> new IllegalArgumentException("Taxa de IVA nao encontrada."));

        taxRate.setName(command.name().trim());
        taxRate.setRate(command.rate());
        taxRate.setDescription(AdminTextSanitizer.trimOrNull(command.description()));
        taxRate.setActive(command.active() == null || command.active());

        TaxRate updatedTaxRate = taxRateRepository.update(taxRate);
        return AdminTaxRateDTO.from(updatedTaxRate);
    }

    private Map<PaymentStatus, Long> countPaymentsByStatus(LocalDateTime periodStart, LocalDateTime periodEnd) {
        Map<PaymentStatus, Long> paymentCountsByStatus = new EnumMap<>(PaymentStatus.class);
        for (PaymentStatus status : PaymentStatus.values()) {
            paymentCountsByStatus.put(status, 0L);
        }

        for (Object[] row : paymentRepository.countByStatus(periodStart, periodEnd)) {
            PaymentStatus status = (PaymentStatus) row[0];
            long count = ((Number) row[1]).longValue();
            paymentCountsByStatus.put(status, count);
        }
        return paymentCountsByStatus;
    }

    private void validateTaxRateCommand(Integer taxRateId, AdminTaxRateCommand command) {
        if (taxRateId == null) {
            throw new IllegalArgumentException("Taxa de IVA invalida.");
        }
        if (command == null) {
            throw new IllegalArgumentException("Dados de taxa invalidos.");
        }
        if (command.rate() == null) {
            throw new IllegalArgumentException("Percentagem de IVA obrigatoria.");
        }
        if (command.rate().compareTo(BigDecimal.ZERO) < 0 || command.rate().compareTo(BigDecimal.ONE) > 0) {
            throw new IllegalArgumentException("Taxa de IVA deve estar entre 0.0000 e 1.0000.");
        }
        if (AdminTextSanitizer.safe(command.name()).isBlank()) {
            throw new IllegalArgumentException("Nome da taxa e obrigatorio.");
        }
    }

    private AdminFinancialPeriod resolvePeriod(AdminFinancialPeriod period) {
        return period == null ? AdminFinancialPeriod.ALL : period;
    }

    private LocalDateTime resolvePeriodStart(AdminFinancialPeriod period) {
        LocalDateTime now = LocalDateTime.now();
        return switch (period) {
            case DAY -> now.minusDays(1);
            case WEEK -> now.minusWeeks(1);
            case MONTH -> now.minusMonths(1);
            case YEAR -> now.minusYears(1);
            case ALL -> null;
        };
    }

    private LocalDateTime resolvePeriodEnd(AdminFinancialPeriod period) {
        if (period == AdminFinancialPeriod.ALL) {
            return null;
        }
        return LocalDateTime.now();
    }
}
