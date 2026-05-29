package com.obar.desktop.admin.sections.financial;

import com.obar.bll.admin.AdminFinancialPeriod;
import com.obar.bll.admin.AdminPaymentByTripDTO;
import com.obar.desktop.admin.shared.AdminFormatUtils;
import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Populates and toggles the payment detail panel in the financial section.
 */
final class FinancialPaymentDetailPanel {

    private static final DateTimeFormatter PAYMENT_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm",
            Locale.forLanguageTag("pt-PT"));

    private final VBox panel;
    private final Label detailInitialsLabel;
    private final Label detailTitleLabel;
    private final Label detailNameLabel;
    private final Label detailEmailLabel;
    private final Label detailStatusLabel;
    private final Label detailRoleLabel;
    private final Label detailPhoneValueLabel;
    private final Label detailCreatedValueLabel;
    private final Label detailCardOneTitleLabel;
    private final Label detailCardOneValueLabel;
    private final Label detailCardTwoTitleLabel;
    private final Label detailCardTwoValueLabel;
    private final Label detailCardThreeTitleLabel;
    private final Label detailCardThreeValueLabel;
    private final Label detailCardFourTitleLabel;
    private final Label detailCardFourValueLabel;
    private final Label detailReferenceTitleLabel;
    private final Label detailReferenceValueLabel;
    private final Label detailExtraOneTitleLabel;
    private final Label detailExtraOneValueLabel;
    private final Label detailExtraTwoTitleLabel;
    private final Label detailExtraTwoValueLabel;
    private final Label detailExtraThreeTitleLabel;
    private final Label detailExtraThreeValueLabel;

    FinancialPaymentDetailPanel(
            VBox panel,
            Label detailInitialsLabel,
            Label detailTitleLabel,
            Label detailNameLabel,
            Label detailEmailLabel,
            Label detailStatusLabel,
            Label detailRoleLabel,
            Label detailPhoneValueLabel,
            Label detailCreatedValueLabel,
            Label detailCardOneTitleLabel,
            Label detailCardOneValueLabel,
            Label detailCardTwoTitleLabel,
            Label detailCardTwoValueLabel,
            Label detailCardThreeTitleLabel,
            Label detailCardThreeValueLabel,
            Label detailCardFourTitleLabel,
            Label detailCardFourValueLabel,
            Label detailReferenceTitleLabel,
            Label detailReferenceValueLabel,
            Label detailExtraOneTitleLabel,
            Label detailExtraOneValueLabel,
            Label detailExtraTwoTitleLabel,
            Label detailExtraTwoValueLabel,
            Label detailExtraThreeTitleLabel,
            Label detailExtraThreeValueLabel) {
        this.panel = panel;
        this.detailInitialsLabel = detailInitialsLabel;
        this.detailTitleLabel = detailTitleLabel;
        this.detailNameLabel = detailNameLabel;
        this.detailEmailLabel = detailEmailLabel;
        this.detailStatusLabel = detailStatusLabel;
        this.detailRoleLabel = detailRoleLabel;
        this.detailPhoneValueLabel = detailPhoneValueLabel;
        this.detailCreatedValueLabel = detailCreatedValueLabel;
        this.detailCardOneTitleLabel = detailCardOneTitleLabel;
        this.detailCardOneValueLabel = detailCardOneValueLabel;
        this.detailCardTwoTitleLabel = detailCardTwoTitleLabel;
        this.detailCardTwoValueLabel = detailCardTwoValueLabel;
        this.detailCardThreeTitleLabel = detailCardThreeTitleLabel;
        this.detailCardThreeValueLabel = detailCardThreeValueLabel;
        this.detailCardFourTitleLabel = detailCardFourTitleLabel;
        this.detailCardFourValueLabel = detailCardFourValueLabel;
        this.detailReferenceTitleLabel = detailReferenceTitleLabel;
        this.detailReferenceValueLabel = detailReferenceValueLabel;
        this.detailExtraOneTitleLabel = detailExtraOneTitleLabel;
        this.detailExtraOneValueLabel = detailExtraOneValueLabel;
        this.detailExtraTwoTitleLabel = detailExtraTwoTitleLabel;
        this.detailExtraTwoValueLabel = detailExtraTwoValueLabel;
        this.detailExtraThreeTitleLabel = detailExtraThreeTitleLabel;
        this.detailExtraThreeValueLabel = detailExtraThreeValueLabel;
    }

    void show(AdminPaymentByTripDTO payment, AdminFinancialPeriod activePeriod) {
        if (payment == null) {
            hide();
            return;
        }

        String taxRate = payment.getTaxRateApplied() == null ? "-"
                : payment.getTaxRateApplied().multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP) + "%";

        setText(detailInitialsLabel, payment.getPaymentId() == null ? "--" : "#" + payment.getPaymentId());
        setText(detailTitleLabel, "Detalhe do pagamento");
        setText(detailNameLabel, AdminFormatUtils.formatPaymentAmount(payment));
        setText(detailEmailLabel, "Cliente: " + AdminFormatUtils.fallback(payment.getClientName()));
        setText(detailStatusLabel, AdminFormatUtils.prettyPaymentStatus(payment.getStatus()));
        setText(detailRoleLabel, AdminFormatUtils.prettyPaymentMethod(payment.getPaymentMethodType()));
        setText(detailPhoneValueLabel,
                "Metodo: " + AdminFormatUtils.prettyPaymentMethod(payment.getPaymentMethodType()));
        setText(detailCreatedValueLabel,
                payment.getPaymentDate() == null ? "-" : PAYMENT_DATE_FORMAT.format(payment.getPaymentDate()));
        setText(detailCardOneTitleLabel, "ID Viagem");
        setText(detailCardOneValueLabel, payment.getTripId() == null ? "-" : "#" + payment.getTripId());
        setText(detailCardTwoTitleLabel, "Valor");
        setText(detailCardTwoValueLabel, AdminFormatUtils.formatPaymentAmount(payment));
        setText(detailCardThreeTitleLabel, "Estado");
        setText(detailCardThreeValueLabel, AdminFormatUtils.prettyPaymentStatus(payment.getStatus()));
        setText(detailCardFourTitleLabel, "Moeda");
        setText(detailCardFourValueLabel, AdminFormatUtils.fallback(payment.getCurrencyCode()));
        setText(detailReferenceTitleLabel, "Taxa aplicada");
        setText(detailReferenceValueLabel, taxRate);
        setText(detailExtraOneTitleLabel, "Motorista");
        setText(detailExtraOneValueLabel, AdminFormatUtils.fallback(payment.getDriverName()));
        setText(detailExtraTwoTitleLabel, "Data pagamento");
        setText(detailExtraTwoValueLabel,
                payment.getPaymentDate() == null ? "-" : PAYMENT_DATE_FORMAT.format(payment.getPaymentDate()));
        setText(detailExtraThreeTitleLabel, "Periodo");
        setText(detailExtraThreeValueLabel, AdminFormatUtils.prettyFinancialPeriod(activePeriod));
        setVisible(true);
    }

    void hide() {
        setVisible(false);
    }

    private void setVisible(boolean visible) {
        if (panel != null) {
            panel.setVisible(visible);
            panel.setManaged(visible);
        }
    }

    private void setText(Label label, String text) {
        if (label != null) {
            label.setText(text == null ? "-" : text);
        }
    }
}
