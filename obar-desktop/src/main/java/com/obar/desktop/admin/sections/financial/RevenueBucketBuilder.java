package com.obar.desktop.admin.sections.financial;

import com.obar.bll.admin.AdminFinancialPeriod;
import com.obar.bll.admin.AdminPaymentByTripDTO;
import com.obar.desktop.admin.shared.AdminFormatUtils;
import com.obar.model.enums.PaymentStatus;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Pure logic for grouping payment amounts into labelled time buckets.
 * No JavaFX dependency — fully testable without a running application.
 */
public final class RevenueBucketBuilder {

    private static final DateTimeFormatter PAYMENT_DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.forLanguageTag("pt-PT"));
    private static final DateTimeFormatter DAY_LABEL_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM", Locale.forLanguageTag("pt-PT"));
    private static final DateTimeFormatter MONTH_LABEL_FORMAT =
            DateTimeFormatter.ofPattern("MMM", Locale.forLanguageTag("pt-PT"));

    public record RevenueBucket(String label, String tooltip, BigDecimal value) {}

    /**
     * Builds the bucket list appropriate for the given period,
     * filtering the input list to PROCESSED payments with non-null dates.
     */
    public List<RevenueBucket> build(AdminFinancialPeriod period, List<AdminPaymentByTripDTO> payments) {
        List<AdminPaymentByTripDTO> processed = payments == null ? List.of() : payments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.PROCESSED)
                .filter(p -> p.getPaymentDate() != null)
                .sorted(Comparator.comparing(AdminPaymentByTripDTO::getPaymentDate))
                .toList();

        return switch (period) {
            case DAY   -> buildDayBuckets(processed);
            case WEEK  -> buildWeekBuckets(processed);
            case MONTH -> buildMonthBuckets(processed);
            case YEAR  -> buildYearBuckets(processed);
            case ALL   -> buildAllBuckets(processed);
        };
    }

    private List<RevenueBucket> buildDayBuckets(List<AdminPaymentByTripDTO> payments) {
        List<RevenueBucket> buckets = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        int bucketHours = 3;
        int bucketCount = 8;
        for (int i = bucketCount - 1; i >= 0; i--) {
            LocalDateTime start = now.minusHours((long) (i + 1) * bucketHours);
            LocalDateTime end   = now.minusHours((long) i * bucketHours);
            BigDecimal value    = sumBetween(payments, start, end);
            String label        = String.format(Locale.ROOT, "%02dh", start.getHour());
            String tooltip      = start.format(PAYMENT_DATE_FORMAT) + " - " + end.format(PAYMENT_DATE_FORMAT);
            buckets.add(new RevenueBucket(label, tooltip, value));
        }
        return buckets;
    }

    private List<RevenueBucket> buildWeekBuckets(List<AdminPaymentByTripDTO> payments) {
        List<RevenueBucket> buckets = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = 6; i >= 0; i--) {
            LocalDate day    = today.minusDays(i);
            BigDecimal value = sumForDay(payments, day);
            String label     = day.format(DAY_LABEL_FORMAT);
            buckets.add(new RevenueBucket(label, "Dia " + label, value));
        }
        return buckets;
    }

    private List<RevenueBucket> buildMonthBuckets(List<AdminPaymentByTripDTO> payments) {
        List<RevenueBucket> buckets = new ArrayList<>();
        LocalDate start = LocalDate.now().minusDays(29);
        for (int i = 0; i < 30; i++) {
            LocalDate day    = start.plusDays(i);
            BigDecimal value = sumForDay(payments, day);
            String label     = String.valueOf(day.getDayOfMonth());
            buckets.add(new RevenueBucket(label, "Dia " + day.format(DAY_LABEL_FORMAT), value));
        }
        return buckets;
    }

    private List<RevenueBucket> buildYearBuckets(List<AdminPaymentByTripDTO> payments) {
        List<RevenueBucket> buckets = new ArrayList<>();
        YearMonth current = YearMonth.now();
        for (int i = 11; i >= 0; i--) {
            YearMonth month      = current.minusMonths(i);
            LocalDateTime start  = month.atDay(1).atStartOfDay();
            LocalDateTime end    = month.plusMonths(1).atDay(1).atStartOfDay();
            BigDecimal value     = sumBetween(payments, start, end);
            String label         = month.format(MONTH_LABEL_FORMAT);
            String tooltip       = month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("pt-PT")));
            buckets.add(new RevenueBucket(label, tooltip, value));
        }
        return buckets;
    }

    private List<RevenueBucket> buildAllBuckets(List<AdminPaymentByTripDTO> payments) {
        List<RevenueBucket> buckets = new ArrayList<>();
        int currentYear = LocalDate.now().getYear();
        int minYear     = payments.stream()
                .mapToInt(p -> p.getPaymentDate().getYear())
                .min()
                .orElse(currentYear - 4);
        int startYear   = Math.min(minYear, currentYear - 4);
        for (int year = startYear; year <= currentYear; year++) {
            LocalDateTime start = LocalDate.of(year, 1, 1).atStartOfDay();
            LocalDateTime end   = LocalDate.of(year + 1, 1, 1).atStartOfDay();
            BigDecimal value    = sumBetween(payments, start, end);
            buckets.add(new RevenueBucket(String.valueOf(year), "Ano " + year, value));
        }
        return buckets;
    }

    private BigDecimal sumForDay(List<AdminPaymentByTripDTO> payments, LocalDate day) {
        return payments.stream()
                .filter(p -> p.getPaymentDate().toLocalDate().equals(day))
                .map(p -> AdminFormatUtils.defaultAmount(p.getAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumBetween(List<AdminPaymentByTripDTO> payments, LocalDateTime start, LocalDateTime end) {
        return payments.stream()
                .filter(p -> !p.getPaymentDate().isBefore(start))
                .filter(p -> p.getPaymentDate().isBefore(end))
                .map(p -> AdminFormatUtils.defaultAmount(p.getAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}
