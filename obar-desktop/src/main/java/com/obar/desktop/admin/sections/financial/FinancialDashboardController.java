package com.obar.desktop.admin.sections.financial;

import com.obar.bll.admin.AdminFinancialOverviewDTO;
import com.obar.bll.admin.AdminFinancialPeriod;
import com.obar.bll.admin.AdminPaymentByTripDTO;
import com.obar.desktop.admin.shared.AdminFormatUtils;
import com.obar.model.enums.PaymentStatus;
import javafx.geometry.Pos;
import javafx.scene.control.Label;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Updates the financial dashboard labels, revenue bars, and payment method
 * summary shown in the financial section.
 */
final class FinancialDashboardController {

    private static final DateTimeFormatter PAYMENT_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm",
            Locale.forLanguageTag("pt-PT"));
    private static final DateTimeFormatter DAY_LABEL_FORMAT = DateTimeFormatter.ofPattern("dd/MM",
            Locale.forLanguageTag("pt-PT"));
    private static final DateTimeFormatter MONTH_LABEL_FORMAT = DateTimeFormatter.ofPattern("MMM",
            Locale.forLanguageTag("pt-PT"));

    private record RevenueBucket(String label, String tooltip, BigDecimal value) {
    }

    private final Label revenuePeriodLabel;
    private final Label revenueTotalLabel;
    private final Label revenueTrendLabel;
    private final Label netRevenueValueLabel;
    private final Label platformCommissionValueLabel;
    private final Label billedTripsValueLabel;
    private final Label avgTicketValueLabel;
    private final Label processedPaymentsValueLabel;
    private final Label refundedPaymentsValueLabel;
    private final Label failedRateValueLabel;
    private final Label paidDriversValueLabel;
    private final HBox dailyBarsContainer;
    private final Label methodOneLabel;
    private final Label methodOnePercentLabel;
    private final Label methodTwoLabel;
    private final Label methodTwoPercentLabel;
    private final Label methodThreeLabel;
    private final Label methodThreePercentLabel;
    private final Label methodFourLabel;
    private final Label methodFourPercentLabel;

    FinancialDashboardController(
            Label revenuePeriodLabel,
            Label revenueTotalLabel,
            Label revenueTrendLabel,
            Label netRevenueValueLabel,
            Label platformCommissionValueLabel,
            Label billedTripsValueLabel,
            Label avgTicketValueLabel,
            Label processedPaymentsValueLabel,
            Label refundedPaymentsValueLabel,
            Label failedRateValueLabel,
            Label paidDriversValueLabel,
            HBox dailyBarsContainer,
            Label methodOneLabel,
            Label methodOnePercentLabel,
            Label methodTwoLabel,
            Label methodTwoPercentLabel,
            Label methodThreeLabel,
            Label methodThreePercentLabel,
            Label methodFourLabel,
            Label methodFourPercentLabel) {
        this.revenuePeriodLabel = revenuePeriodLabel;
        this.revenueTotalLabel = revenueTotalLabel;
        this.revenueTrendLabel = revenueTrendLabel;
        this.netRevenueValueLabel = netRevenueValueLabel;
        this.platformCommissionValueLabel = platformCommissionValueLabel;
        this.billedTripsValueLabel = billedTripsValueLabel;
        this.avgTicketValueLabel = avgTicketValueLabel;
        this.processedPaymentsValueLabel = processedPaymentsValueLabel;
        this.refundedPaymentsValueLabel = refundedPaymentsValueLabel;
        this.failedRateValueLabel = failedRateValueLabel;
        this.paidDriversValueLabel = paidDriversValueLabel;
        this.dailyBarsContainer = dailyBarsContainer;
        this.methodOneLabel = methodOneLabel;
        this.methodOnePercentLabel = methodOnePercentLabel;
        this.methodTwoLabel = methodTwoLabel;
        this.methodTwoPercentLabel = methodTwoPercentLabel;
        this.methodThreeLabel = methodThreeLabel;
        this.methodThreePercentLabel = methodThreePercentLabel;
        this.methodFourLabel = methodFourLabel;
        this.methodFourPercentLabel = methodFourPercentLabel;
    }

    void update(AdminFinancialPeriod period, AdminFinancialOverviewDTO overview, List<AdminPaymentByTripDTO> payments) {
        if (overview == null) {
            renderEmpty(period);
            return;
        }

        BigDecimal periodIncome = AdminFormatUtils.defaultAmount(overview.getPeriodIncome());
        BigDecimal commission = periodIncome.multiply(new BigDecimal("0.20"));
        long processed = overview.getProcessedPayments();
        long total = Math.max(overview.getTotalPayments(), 0);
        long failed = overview.getFailedPayments();
        BigDecimal avgTicket = processed <= 0
                ? BigDecimal.ZERO
                : periodIncome.divide(BigDecimal.valueOf(processed), 2, RoundingMode.HALF_UP);
        double processedRate = total == 0 ? 0 : (processed * 100.0) / total;
        double failedRate = total == 0 ? 0 : (failed * 100.0) / total;

        setText(revenuePeriodLabel, "Receita Total - " + AdminFormatUtils.prettyFinancialPeriod(period));
        setText(revenueTotalLabel, AdminFormatUtils.formatCurrency(periodIncome));
        setText(revenueTrendLabel,
                "Taxa de sucesso: " + AdminFormatUtils.formatPercent(processedRate) + " | Pagamentos: " + total);
        setText(netRevenueValueLabel, AdminFormatUtils.formatCurrency(periodIncome));
        setText(platformCommissionValueLabel, AdminFormatUtils.formatCurrency(commission));
        setText(billedTripsValueLabel, String.valueOf(processed));
        setText(avgTicketValueLabel, AdminFormatUtils.formatCurrency(avgTicket));
        setText(processedPaymentsValueLabel, String.valueOf(processed));
        setText(refundedPaymentsValueLabel, String.valueOf(overview.getRefundedPayments()));
        setText(failedRateValueLabel, AdminFormatUtils.formatPercent(failedRate));
        setText(paidDriversValueLabel, String.valueOf(countPaidDrivers(payments)));

        renderRevenueBars(period, payments);
        updatePaymentMethodsLegend(groupPaymentMethods(payments), total);
    }

    private void renderEmpty(AdminFinancialPeriod period) {
        setText(revenuePeriodLabel, "Receita Total");
        setText(revenueTotalLabel, "EUR 0.00");
        setText(revenueTrendLabel, "Sem dados no periodo selecionado.");
        setText(netRevenueValueLabel, "EUR 0.00");
        setText(platformCommissionValueLabel, "EUR 0.00");
        setText(billedTripsValueLabel, "0");
        setText(avgTicketValueLabel, "EUR 0.00");
        setText(processedPaymentsValueLabel, "0");
        setText(refundedPaymentsValueLabel, "0");
        setText(failedRateValueLabel, "0.0%");
        setText(paidDriversValueLabel, "0");
        renderRevenueBars(period == null ? AdminFinancialPeriod.MONTH : period, List.of());
        updatePaymentMethodsLegend(new HashMap<>(), 0L);
    }

    static Map<String, Long> groupPaymentMethods(List<AdminPaymentByTripDTO> payments) {
        Map<String, Long> grouped = new HashMap<>();
        if (payments == null) {
            return grouped;
        }
        for (AdminPaymentByTripDTO payment : payments) {
            String key = AdminFormatUtils
                    .normalize(AdminFormatUtils.prettyPaymentMethod(payment.getPaymentMethodType()));
            grouped.put(key, grouped.getOrDefault(key, 0L) + 1);
        }
        return grouped;
    }

    private long countPaidDrivers(List<AdminPaymentByTripDTO> payments) {
        if (payments == null) {
            return 0;
        }
        return payments.stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.PROCESSED)
                .map(payment -> AdminFormatUtils.normalize(payment.getDriverName()))
                .filter(name -> !name.isBlank())
                .distinct()
                .count();
    }

    private void updatePaymentMethodsLegend(Map<String, Long> groupedMethods, long totalPayments) {
        List<Map.Entry<String, Long>> sorted = groupedMethods.entrySet().stream()
                .sorted((left, right) -> Long.compare(right.getValue(), left.getValue()))
                .limit(4)
                .toList();
        setLegendRow(sorted, 0, methodOneLabel, methodOnePercentLabel, totalPayments);
        setLegendRow(sorted, 1, methodTwoLabel, methodTwoPercentLabel, totalPayments);
        setLegendRow(sorted, 2, methodThreeLabel, methodThreePercentLabel, totalPayments);
        setLegendRow(sorted, 3, methodFourLabel, methodFourPercentLabel, totalPayments);
    }

    private void setLegendRow(List<Map.Entry<String, Long>> sorted, int index, Label methodLabel, Label percentLabel,
            long total) {
        if (index >= sorted.size()) {
            setText(methodLabel, "-");
            setText(percentLabel, "0%");
            return;
        }
        Map.Entry<String, Long> entry = sorted.get(index);
        setText(methodLabel, AdminFormatUtils.prettyPaymentMethod(entry.getKey()));
        setText(percentLabel, AdminFormatUtils.formatPercent(total == 0 ? 0 : (entry.getValue() * 100.0) / total));
    }

    private void renderRevenueBars(AdminFinancialPeriod period, List<AdminPaymentByTripDTO> payments) {
        if (dailyBarsContainer == null) {
            return;
        }
        dailyBarsContainer.getChildren().clear();
        List<RevenueBucket> buckets = buildRevenueBuckets(period, payments);
        BigDecimal maxValue = buckets.stream().map(RevenueBucket::value).reduce(BigDecimal.ZERO, BigDecimal::max);

        if (maxValue.compareTo(BigDecimal.ZERO) == 0) {
            dailyBarsContainer.setAlignment(Pos.CENTER_LEFT);
            Label empty = new Label("Sem receita processada no periodo selecionado.");
            empty.getStyleClass().add("financial-empty-state");
            dailyBarsContainer.getChildren().add(empty);
            return;
        }

        dailyBarsContainer.setAlignment(Pos.BOTTOM_LEFT);
        double barWidth = buckets.size() <= 8 ? 26 : buckets.size() <= 12 ? 20 : buckets.size() <= 20 ? 14 : 9;
        for (RevenueBucket bucket : buckets) {
            double ratio = bucket.value().divide(maxValue, 4, RoundingMode.HALF_UP).doubleValue();
            VBox column = new VBox(4.0);
            column.setAlignment(Pos.BOTTOM_CENTER);
            column.getStyleClass().add("financial-bar-column");

            Label valueLabel = new Label(formatCompactCurrency(bucket.value()));
            valueLabel.getStyleClass().add("financial-bar-value");

            Region bar = new Region();
            bar.getStyleClass().add("financial-bar");
            if (ratio >= 0.75) {
                bar.getStyleClass().add("financial-bar-strong");
            } else if (ratio >= 0.45) {
                bar.getStyleClass().add("financial-bar-medium");
            }
            bar.setPrefWidth(barWidth);
            bar.setMinWidth(barWidth);
            bar.setMaxWidth(barWidth);
            bar.setPrefHeight(4 + (ratio * 68));

            Label bucketLabel = new Label(bucket.label());
            bucketLabel.getStyleClass().add("financial-bar-label");
            Tooltip.install(bar,
                    new Tooltip(bucket.tooltip() + " | " + AdminFormatUtils.formatCurrency(bucket.value())));

            column.getChildren().addAll(valueLabel, bar, bucketLabel);
            dailyBarsContainer.getChildren().add(column);
        }
    }

    private List<RevenueBucket> buildRevenueBuckets(AdminFinancialPeriod period, List<AdminPaymentByTripDTO> payments) {
        List<AdminPaymentByTripDTO> processed = payments == null ? List.of()
                : payments.stream()
                        .filter(payment -> payment.getStatus() == PaymentStatus.PROCESSED)
                        .filter(payment -> payment.getPaymentDate() != null)
                        .sorted(Comparator.comparing(AdminPaymentByTripDTO::getPaymentDate))
                        .toList();

        return switch (period) {
            case DAY -> buildDayBuckets(processed);
            case WEEK -> buildWeekBuckets(processed);
            case MONTH -> buildMonthBuckets(processed);
            case YEAR -> buildYearBuckets(processed);
            case ALL -> buildAllBuckets(processed);
        };
    }

    private List<RevenueBucket> buildDayBuckets(List<AdminPaymentByTripDTO> payments) {
        List<RevenueBucket> buckets = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        int bucketHours = 3;
        for (int i = 7; i >= 0; i--) {
            LocalDateTime start = now.minusHours((long) (i + 1) * bucketHours);
            LocalDateTime end = now.minusHours((long) i * bucketHours);
            BigDecimal value = sumBetween(payments, start, end);
            String label = String.format(Locale.ROOT, "%02dh", start.getHour());
            buckets.add(new RevenueBucket(label,
                    start.format(PAYMENT_DATE_FORMAT) + " - " + end.format(PAYMENT_DATE_FORMAT), value));
        }
        return buckets;
    }

    private List<RevenueBucket> buildWeekBuckets(List<AdminPaymentByTripDTO> payments) {
        List<RevenueBucket> buckets = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = 6; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            BigDecimal value = sumForDay(payments, day);
            String label = day.format(DAY_LABEL_FORMAT);
            buckets.add(new RevenueBucket(label, "Dia " + label, value));
        }
        return buckets;
    }

    private List<RevenueBucket> buildMonthBuckets(List<AdminPaymentByTripDTO> payments) {
        List<RevenueBucket> buckets = new ArrayList<>();
        LocalDate start = LocalDate.now().minusDays(29);
        for (int i = 0; i < 30; i++) {
            LocalDate day = start.plusDays(i);
            BigDecimal value = sumForDay(payments, day);
            buckets.add(new RevenueBucket(String.valueOf(day.getDayOfMonth()), "Dia " + day.format(DAY_LABEL_FORMAT),
                    value));
        }
        return buckets;
    }

    private List<RevenueBucket> buildYearBuckets(List<AdminPaymentByTripDTO> payments) {
        List<RevenueBucket> buckets = new ArrayList<>();
        YearMonth current = YearMonth.now();
        for (int i = 11; i >= 0; i--) {
            YearMonth month = current.minusMonths(i);
            LocalDateTime start = month.atDay(1).atStartOfDay();
            LocalDateTime end = month.plusMonths(1).atDay(1).atStartOfDay();
            BigDecimal value = sumBetween(payments, start, end);
            String label = month.format(MONTH_LABEL_FORMAT);
            String tooltip = month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("pt-PT")));
            buckets.add(new RevenueBucket(label, tooltip, value));
        }
        return buckets;
    }

    private List<RevenueBucket> buildAllBuckets(List<AdminPaymentByTripDTO> payments) {
        List<RevenueBucket> buckets = new ArrayList<>();
        int currentYear = LocalDate.now().getYear();
        int minYear = payments.stream().mapToInt(payment -> payment.getPaymentDate().getYear()).min()
                .orElse(currentYear - 4);
        int startYear = Math.min(minYear, currentYear - 4);
        for (int year = startYear; year <= currentYear; year++) {
            LocalDateTime start = LocalDate.of(year, 1, 1).atStartOfDay();
            LocalDateTime end = LocalDate.of(year + 1, 1, 1).atStartOfDay();
            buckets.add(new RevenueBucket(String.valueOf(year), "Ano " + year, sumBetween(payments, start, end)));
        }
        return buckets;
    }

    private BigDecimal sumForDay(List<AdminPaymentByTripDTO> payments, LocalDate day) {
        return payments.stream()
                .filter(payment -> payment.getPaymentDate().toLocalDate().equals(day))
                .map(payment -> AdminFormatUtils.defaultAmount(payment.getAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumBetween(List<AdminPaymentByTripDTO> payments, LocalDateTime start, LocalDateTime end) {
        return payments.stream()
                .filter(payment -> !payment.getPaymentDate().isBefore(start))
                .filter(payment -> payment.getPaymentDate().isBefore(end))
                .map(payment -> AdminFormatUtils.defaultAmount(payment.getAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String formatCompactCurrency(BigDecimal value) {
        BigDecimal safe = AdminFormatUtils.defaultAmount(value);
        if (safe.compareTo(BigDecimal.ZERO) == 0) {
            return "EUR 0";
        }
        if (safe.compareTo(new BigDecimal("1000")) >= 0) {
            return "EUR " + safe.divide(new BigDecimal("1000"), 1, RoundingMode.HALF_UP) + "k";
        }
        return "EUR " + safe.setScale(0, RoundingMode.HALF_UP);
    }

    private void setText(Label label, String text) {
        if (label != null) {
            label.setText(text == null ? "-" : text);
        }
    }
}
