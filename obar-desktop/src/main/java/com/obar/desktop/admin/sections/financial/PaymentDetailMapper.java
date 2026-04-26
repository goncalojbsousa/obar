package com.obar.desktop.admin.sections.financial;

import com.obar.bll.admin.AdminPaymentByTripDTO;
import com.obar.bll.admin.AdminTaxRateDTO;
import com.obar.desktop.admin.shared.AdminFormatUtils;
import com.obar.desktop.admin.shared.DetailPanelBinder.DetailViewModel;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

/**
 * Maps financial data to {@link DetailViewModel} instances.
 *
 * <p>
 * Three mappings are needed in the financial section:
 * <ul>
 * <li>{@link #fromPayment} - single payment selection
 * <li>{@link #fromOverview} - summary panel shown when no payment is selected
 * <li>{@link #empty} - placeholder when there is no data at all
 * </ul>
 */
public final class PaymentDetailMapper {

        private static final DateTimeFormatter PAYMENT_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm",
                        Locale.forLanguageTag("pt-PT"));

        private PaymentDetailMapper() {
                throw new UnsupportedOperationException("Utility class");
        }

        /** Maps a selected payment to a detail view. */
        public static DetailViewModel fromPayment(AdminPaymentByTripDTO payment, FinancialViewModel viewModel) {
                String taxRate = payment.getTaxRateApplied() == null ? "-"
                                : payment.getTaxRateApplied()
                                                .multiply(new BigDecimal("100"))
                                                .setScale(2, RoundingMode.HALF_UP) + "%";

                return new DetailViewModel.Builder()
                                .initials(payment.getPaymentId() == null ? "--" : "#" + payment.getPaymentId())
                                .title("Detalhe do pagamento")
                                .name(AdminFormatUtils.formatPaymentAmount(payment))
                                .email("Cliente: " + AdminFormatUtils.fallback(payment.getClientName()))
                                .status(AdminFormatUtils.prettyPaymentStatus(payment.getStatus()))
                                .role(AdminFormatUtils.prettyPaymentMethod(payment.getPaymentMethodType()))
                                .phone("Metodo: "
                                                + AdminFormatUtils.prettyPaymentMethod(payment.getPaymentMethodType()))
                                .created(payment.getPaymentDate() == null ? "-"
                                                : PAYMENT_DATE_FORMAT.format(payment.getPaymentDate()))
                                .card1("ID Viagem", payment.getTripId() == null ? "-" : "#" + payment.getTripId())
                                .card2("Valor", AdminFormatUtils.formatPaymentAmount(payment))
                                .card3("Estado", AdminFormatUtils.prettyPaymentStatus(payment.getStatus()))
                                .card4("Moeda", AdminFormatUtils.fallback(payment.getCurrencyCode()))
                                .ref("Taxa aplicada", taxRate)
                                .extra1("Motorista", AdminFormatUtils.fallback(payment.getDriverName()))
                                .extra2("Data pagamento",
                                                payment.getPaymentDate() == null ? "-"
                                                                : PAYMENT_DATE_FORMAT.format(payment.getPaymentDate()))
                                .extra3("Periodo", AdminFormatUtils.prettyFinancialPeriod(viewModel.getActivePeriod()))
                                .build();
        }

        /** Maps the current overview + tax rates to the summary panel. */
        public static DetailViewModel fromOverview(FinancialViewModel viewModel) {
                if (viewModel.getCurrentOverview() == null) {
                        return empty();
                }

                List<AdminTaxRateDTO> taxRates = viewModel.getAllTaxRates();
                AdminTaxRateDTO topTax = taxRates.stream()
                                .max(Comparator.comparing(AdminTaxRateDTO::getRate,
                                                Comparator.nullsLast(Comparator.naturalOrder())))
                                .orElse(null);

                var overview = viewModel.getCurrentOverview();
                return new DetailViewModel.Builder()
                                .initials("EUR")
                                .title("Resumo financeiro")
                                .name("Periodo: " + AdminFormatUtils.prettyFinancialPeriod(viewModel.getActivePeriod()))
                                .email("Total pagamentos: " + overview.getTotalPayments())
                                .status("Processados: " + overview.getProcessedPayments())
                                .role("Financeiro")
                                .phone("Pendentes: " + overview.getPendingPayments())
                                .created(LocalDateTime.now().format(PAYMENT_DATE_FORMAT))
                                .card1("Rend. total",
                                                AdminFormatUtils.formatCurrency(AdminFormatUtils
                                                                .defaultAmount(overview.getTotalIncome())))
                                .card2("Rend. periodo",
                                                AdminFormatUtils.formatCurrency(AdminFormatUtils
                                                                .defaultAmount(overview.getPeriodIncome())))
                                .card3("Processados", String.valueOf(overview.getProcessedPayments()))
                                .card4("Pendentes", String.valueOf(overview.getPendingPayments()))
                                .ref("Falhados", String.valueOf(overview.getFailedPayments()))
                                .extra1("Reembolsados", String.valueOf(overview.getRefundedPayments()))
                                .extra2("Top IVA", topTax == null ? "-" : AdminFormatUtils.formatTaxRateDisplay(topTax))
                                .extra3("Acao", "Use exportar para gerar relatorio.")
                                .build();
        }

        /** Empty/placeholder state when there is no data or selection. */
        public static DetailViewModel empty() {
                return new DetailViewModel.Builder()
                                .initials("EUR")
                                .title("Resumo financeiro")
                                .name("Sem dados financeiros")
                                .role("Financeiro")
                                .card1("Rend. total", "-")
                                .card2("Rend. periodo", "-")
                                .card3("Processados", "-")
                                .card4("Pendentes", "-")
                                .ref("Falhados", "-")
                                .extra1("Reembolsados", "-")
                                .extra2("Top IVA", "-")
                                .extra3("Acao", "-")
                                .build();
        }
}
