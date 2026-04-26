package com.obar.desktop.admin.sections.financial;

import com.obar.bll.admin.AdminFinancialOverviewDTO;
import com.obar.bll.admin.AdminFinancialPeriod;
import com.obar.bll.admin.AdminPaymentByTripDTO;
import com.obar.desktop.admin.sections.financial.RevenueBucketBuilder.RevenueBucket;
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
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Presenter responsible for rendering financial dashboard/list summaries.
 */
public final class FinancialDashboardPresenter {

    private final RevenueBucketBuilder bucketBuilder = new RevenueBucketBuilder();

    public void updateFilterSummary(FinancialViewModel viewModel, Label tableTitle, Label listInfoLabel) {
        long pending = viewModel.getAllPayments().stream().filter(p -> p.getStatus() == PaymentStatus.PENDING).count();
        long processed = viewModel.getAllPayments().stream().filter(p -> p.getStatus() == PaymentStatus.PROCESSED)
                .count();
        long failed = viewModel.getAllPayments().stream().filter(p -> p.getStatus() == PaymentStatus.FAILED).count();
        long refunded = viewModel.getAllPayments().stream().filter(p -> p.getStatus() == PaymentStatus.REFUNDED)
                .count();

        if (listInfoLabel != null) {
            listInfoLabel.setText(
                    "A mostrar " + viewModel.getFilteredPayments().size() + " de " + viewModel.getAllPayments().size()
                            + " pagamentos");
        }
        tableTitle.setText("Ultimos Pagamentos"
                + " | Proc: " + processed
                + " Pend: " + pending
                + " Falh: " + failed
                + " Reemb: " + refunded);
    }

    public void updateDashboard(
            FinancialViewModel viewModel,
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
        AdminFinancialOverviewDTO overview = viewModel.getCurrentOverview();
        if (overview == null) {
            renderEmptyDashboard(
                    revenuePeriodLabel, revenueTotalLabel, revenueTrendLabel,
                    netRevenueValueLabel, platformCommissionValueLabel, billedTripsValueLabel,
                    avgTicketValueLabel, processedPaymentsValueLabel, refundedPaymentsValueLabel,
                    failedRateValueLabel, paidDriversValueLabel, dailyBarsContainer,
                    methodOneLabel, methodOnePercentLabel, methodTwoLabel, methodTwoPercentLabel,
                    methodThreeLabel, methodThreePercentLabel, methodFourLabel, methodFourPercentLabel);
            return;
        }

        BigDecimal periodIncome = AdminFormatUtils.defaultAmount(overview.getPeriodIncome());
        BigDecimal commission = periodIncome.multiply(new BigDecimal("0.20"));
        long processed = overview.getProcessedPayments();
        long total = Math.max(overview.getTotalPayments(), 0);
        long refunded = overview.getRefundedPayments();
        long failed = overview.getFailedPayments();
        BigDecimal avgTicket = processed <= 0
                ? BigDecimal.ZERO
                : periodIncome.divide(BigDecimal.valueOf(processed), 2, RoundingMode.HALF_UP);
        double processedRate = total == 0 ? 0 : (processed * 100.0) / total;
        double failedRate = total == 0 ? 0 : (failed * 100.0) / total;

        revenuePeriodLabel
                .setText("Receita Total - " + AdminFormatUtils.prettyFinancialPeriod(viewModel.getActivePeriod()));
        revenueTotalLabel.setText(AdminFormatUtils.formatCurrency(periodIncome));
        revenueTrendLabel.setText(
                "Taxa de sucesso: " + AdminFormatUtils.formatPercent(processedRate) + " | Pagamentos: " + total);
        netRevenueValueLabel.setText(AdminFormatUtils.formatCurrency(periodIncome));
        platformCommissionValueLabel.setText(AdminFormatUtils.formatCurrency(commission));
        billedTripsValueLabel.setText(String.valueOf(processed));
        avgTicketValueLabel.setText(AdminFormatUtils.formatCurrency(avgTicket));
        processedPaymentsValueLabel.setText(String.valueOf(processed));
        refundedPaymentsValueLabel.setText(String.valueOf(refunded));
        failedRateValueLabel.setText(AdminFormatUtils.formatPercent(failedRate));
        paidDriversValueLabel.setText(String.valueOf(viewModel.countPaidDrivers()));

        renderRevenueBars(viewModel, dailyBarsContainer);
        updatePaymentMethodsLegend(viewModel.groupPaymentMethods(), total,
                methodOneLabel, methodOnePercentLabel,
                methodTwoLabel, methodTwoPercentLabel,
                methodThreeLabel, methodThreePercentLabel,
                methodFourLabel, methodFourPercentLabel);
    }

    private void renderEmptyDashboard(
            Label revenuePeriodLabel, Label revenueTotalLabel, Label revenueTrendLabel,
            Label netRevenueValueLabel, Label platformCommissionValueLabel, Label billedTripsValueLabel,
            Label avgTicketValueLabel, Label processedPaymentsValueLabel, Label refundedPaymentsValueLabel,
            Label failedRateValueLabel, Label paidDriversValueLabel, HBox dailyBarsContainer,
            Label methodOneLabel, Label methodOnePercentLabel, Label methodTwoLabel, Label methodTwoPercentLabel,
            Label methodThreeLabel, Label methodThreePercentLabel, Label methodFourLabel,
            Label methodFourPercentLabel) {
        revenuePeriodLabel.setText("Receita Total");
        revenueTotalLabel.setText("EUR 0.00");
        revenueTrendLabel.setText("Sem dados no periodo selecionado.");
        netRevenueValueLabel.setText("EUR 0.00");
        platformCommissionValueLabel.setText("EUR 0.00");
        billedTripsValueLabel.setText("0");
        avgTicketValueLabel.setText("EUR 0.00");
        processedPaymentsValueLabel.setText("0");
        refundedPaymentsValueLabel.setText("0");
        failedRateValueLabel.setText("0.0%");
        paidDriversValueLabel.setText("0");
        renderRevenueBars(AdminFinancialPeriod.MONTH, List.of(), dailyBarsContainer);
        updatePaymentMethodsLegend(new HashMap<>(), 0L,
                methodOneLabel, methodOnePercentLabel,
                methodTwoLabel, methodTwoPercentLabel,
                methodThreeLabel, methodThreePercentLabel,
                methodFourLabel, methodFourPercentLabel);
    }

    private void renderRevenueBars(FinancialViewModel viewModel, HBox dailyBarsContainer) {
        renderRevenueBars(viewModel.getActivePeriod(), viewModel.getPaymentsSnapshot(), dailyBarsContainer);
    }

    private void renderRevenueBars(AdminFinancialPeriod period, List<AdminPaymentByTripDTO> payments,
            HBox dailyBarsContainer) {
        dailyBarsContainer.getChildren().clear();
        List<RevenueBucket> buckets = bucketBuilder.build(period, payments);
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
            bar.setPrefHeight(6 + (ratio * 88));

            Label bucketLabel = new Label(bucket.label());
            bucketLabel.getStyleClass().add("financial-bar-label");
            Tooltip.install(bar,
                    new Tooltip(bucket.tooltip() + " | " + AdminFormatUtils.formatCurrency(bucket.value())));

            column.getChildren().addAll(valueLabel, bar, bucketLabel);
            dailyBarsContainer.getChildren().add(column);
        }
    }

    private void updatePaymentMethodsLegend(
            Map<String, Long> groupedMethods,
            long totalPayments,
            Label methodOneLabel,
            Label methodOnePercentLabel,
            Label methodTwoLabel,
            Label methodTwoPercentLabel,
            Label methodThreeLabel,
            Label methodThreePercentLabel,
            Label methodFourLabel,
            Label methodFourPercentLabel) {
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
            methodLabel.setText("-");
            percentLabel.setText("0%");
            return;
        }
        Map.Entry<String, Long> entry = sorted.get(index);
        methodLabel.setText(AdminFormatUtils.prettyPaymentMethod(entry.getKey()));
        percentLabel.setText(AdminFormatUtils.formatPercent(total == 0 ? 0 : (entry.getValue() * 100.0) / total));
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
}
