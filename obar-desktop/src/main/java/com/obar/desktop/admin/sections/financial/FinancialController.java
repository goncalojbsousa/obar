package com.obar.desktop.admin.sections.financial;

import com.obar.bll.admin.AdminFinancialOverviewDTO;
import com.obar.bll.admin.AdminFinancialPeriod;
import com.obar.bll.admin.AdminPaymentByTripDTO;
import com.obar.bll.admin.AdminService;
import com.obar.bll.admin.AdminTaxRateDTO;
import com.obar.desktop.admin.sections.AdminSectionController;
import com.obar.desktop.admin.shared.AdminFormatUtils;
import com.obar.desktop.admin.shared.AdminModalIncludeController;
import com.obar.model.enums.PaymentStatus;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

/**
 * Controller for the Financial admin section.
 *
 * <p>
 * Loads financial data from {@link AdminService}, filters the payments table,
 * updates dashboard labels and charts, edits IVA/tax rates, and delegates
 * CSV/PDF generation to {@link FinancialExportService}.
 * </p>
 */
public class FinancialController implements AdminSectionController {

    private static final DateTimeFormatter PAYMENT_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm",
            Locale.forLanguageTag("pt-PT"));
    private static final DateTimeFormatter EXPORT_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss",
            Locale.ROOT);

    private final FinancialExportService exportService = new FinancialExportService();
    private final ObservableList<AdminPaymentByTripDTO> allPayments = FXCollections.observableArrayList();
    private final FilteredList<AdminPaymentByTripDTO> filteredPayments = new FilteredList<>(allPayments,
            payment -> true);
    private final ObservableList<AdminTaxRateDTO> allTaxRates = FXCollections.observableArrayList();

    private AdminService adminService;
    private AdminFinancialPeriod activePeriod = AdminFinancialPeriod.MONTH;
    private PaymentStatus activePaymentStatusFilter;
    private AdminFinancialOverviewDTO currentOverview;
    private FinancialDashboardController dashboardController;

    @FXML
    private TextField searchField;
    @FXML
    private TableView<AdminPaymentByTripDTO> paymentsTable;
    @FXML
    private TableColumn<AdminPaymentByTripDTO, String> paymentIdColumn;
    @FXML
    private TableColumn<AdminPaymentByTripDTO, String> paymentTripIdColumn;
    @FXML
    private TableColumn<AdminPaymentByTripDTO, String> paymentClientColumn;
    @FXML
    private TableColumn<AdminPaymentByTripDTO, String> paymentMethodColumn;
    @FXML
    private TableColumn<AdminPaymentByTripDTO, String> paymentAmountColumn;
    @FXML
    private TableColumn<AdminPaymentByTripDTO, String> paymentStatusColumn;
    @FXML
    private TableColumn<AdminPaymentByTripDTO, String> paymentDateColumn;
    @FXML
    private Label paymentsTableTitle;
    @FXML
    private Label listInfoLabel;
    @FXML
    private Label feedbackLabel;
    @FXML
    private Button exportPdfButton;
    @FXML
    private Button allPaymentsFilterButton;
    @FXML
    private Button processedPaymentsFilterButton;
    @FXML
    private Button failedPaymentsFilterButton;
    @FXML
    private Button refundedPaymentsFilterButton;
    @FXML
    private Button pendingPaymentsFilterButton;
    @FXML
    private Button periodDayButton;
    @FXML
    private Button periodWeekButton;
    @FXML
    private Button periodMonthButton;
    @FXML
    private Button periodYearButton;
    @FXML
    private Button periodAllButton;
    @FXML
    private Label revenuePeriodLabel;
    @FXML
    private Label revenueTotalLabel;
    @FXML
    private Label revenueTrendLabel;
    @FXML
    private Label netRevenueValueLabel;
    @FXML
    private Label platformCommissionValueLabel;
    @FXML
    private Label billedTripsValueLabel;
    @FXML
    private Label avgTicketValueLabel;
    @FXML
    private Label processedPaymentsValueLabel;
    @FXML
    private Label refundedPaymentsValueLabel;
    @FXML
    private Label failedRateValueLabel;
    @FXML
    private Label paidDriversValueLabel;
    @FXML
    private HBox dailyBarsContainer;
    @FXML
    private Label methodOneLabel;
    @FXML
    private Label methodOnePercentLabel;
    @FXML
    private Label methodTwoLabel;
    @FXML
    private Label methodTwoPercentLabel;
    @FXML
    private Label methodThreeLabel;
    @FXML
    private Label methodThreePercentLabel;
    @FXML
    private Label methodFourLabel;
    @FXML
    private Label methodFourPercentLabel;

    @FXML
    private VBox detailPanel;
    @FXML
    private Label detailInitialsLabel;
    @FXML
    private Label detailTitleLabel;
    @FXML
    private Label detailNameLabel;
    @FXML
    private Label detailEmailLabel;
    @FXML
    private Label detailStatusLabel;
    @FXML
    private Label detailRoleLabel;
    @FXML
    private Label detailPhoneValueLabel;
    @FXML
    private Label detailCreatedValueLabel;
    @FXML
    private Label detailCardOneTitleLabel;
    @FXML
    private Label detailCardOneValueLabel;
    @FXML
    private Label detailCardTwoTitleLabel;
    @FXML
    private Label detailCardTwoValueLabel;
    @FXML
    private Label detailCardThreeTitleLabel;
    @FXML
    private Label detailCardThreeValueLabel;
    @FXML
    private Label detailCardFourTitleLabel;
    @FXML
    private Label detailCardFourValueLabel;
    @FXML
    private Label detailReferenceTitleLabel;
    @FXML
    private Label detailReferenceValueLabel;
    @FXML
    private Label detailExtraOneTitleLabel;
    @FXML
    private Label detailExtraOneValueLabel;
    @FXML
    private Label detailExtraTwoTitleLabel;
    @FXML
    private Label detailExtraTwoValueLabel;
    @FXML
    private Label detailExtraThreeTitleLabel;
    @FXML
    private Label detailExtraThreeValueLabel;

    @FXML
    private AdminModalIncludeController sharedModalController;

    @Override
    public void setAdminService(AdminService adminService) {
        this.adminService = adminService;
        if (paymentsTable != null) {
            reloadFinancialData();
        }
    }

    @Override
    public void onSectionActivated() {
        reloadFinancialData();
        hidePaymentDetailPanel();
    }

    @FXML
    public void initialize() {
        configureTaxRateModal();
        configurePaymentsTableColumns();
        configureDashboard();

        paymentsTable.setItems(filteredPayments);
        searchField.textProperty().addListener((obs, oldText, newText) -> applyPaymentFilter());
        paymentsTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, previous, selected) -> showPaymentDetails(selected));
        allPayments
                .addListener((javafx.collections.ListChangeListener<AdminPaymentByTripDTO>) change -> refreshUiState());
        filteredPayments
                .addListener((javafx.collections.ListChangeListener<AdminPaymentByTripDTO>) change -> refreshUiState());

        hidePaymentDetailPanel();
        refreshUiState();
    }

    private void configureDashboard() {
        dashboardController = new FinancialDashboardController(
                revenuePeriodLabel,
                revenueTotalLabel,
                revenueTrendLabel,
                netRevenueValueLabel,
                platformCommissionValueLabel,
                billedTripsValueLabel,
                avgTicketValueLabel,
                processedPaymentsValueLabel,
                refundedPaymentsValueLabel,
                failedRateValueLabel,
                paidDriversValueLabel,
                dailyBarsContainer,
                methodOneLabel,
                methodOnePercentLabel,
                methodTwoLabel,
                methodTwoPercentLabel,
                methodThreeLabel,
                methodThreePercentLabel,
                methodFourLabel,
                methodFourPercentLabel);
    }

    private void configureTaxRateModal() {
        sharedModalController.bindActions(this::handleModalCancel, this::handleModalSave,
                this::handleModalConfirmDelete);
        var taxRateCombo = sharedModalController.modalTaxRateCombo;
        taxRateCombo.setConverter(new TaxRateStringConverter());
        taxRateCombo.setCellFactory(listView -> new TaxRateListCell());
        taxRateCombo.setButtonCell(new TaxRateListCell());
        taxRateCombo.valueProperty()
                .addListener((obs, oldTaxRate, newTaxRate) -> fillTaxRateForm(newTaxRate));
    }

    @FXML
    public void handleFinancialPeriodDay() {
        setPeriod(AdminFinancialPeriod.DAY);
    }

    @FXML
    public void handleFinancialPeriodWeek() {
        setPeriod(AdminFinancialPeriod.WEEK);
    }

    @FXML
    public void handleFinancialPeriodMonth() {
        setPeriod(AdminFinancialPeriod.MONTH);
    }

    @FXML
    public void handleFinancialPeriodYear() {
        setPeriod(AdminFinancialPeriod.YEAR);
    }

    @FXML
    public void handleFinancialPeriodAll() {
        setPeriod(AdminFinancialPeriod.ALL);
    }

    @FXML
    public void handleShowAllPayments() {
        setPaymentStatusFilter(null);
    }

    @FXML
    public void handleShowProcessedPayments() {
        setPaymentStatusFilter(PaymentStatus.PROCESSED);
    }

    @FXML
    public void handleShowFailedPayments() {
        setPaymentStatusFilter(PaymentStatus.FAILED);
    }

    @FXML
    public void handleShowRefundedPayments() {
        setPaymentStatusFilter(PaymentStatus.REFUNDED);
    }

    @FXML
    public void handleShowPendingPayments() {
        setPaymentStatusFilter(PaymentStatus.PENDING);
    }

    @FXML
    public void handleEditTaxRate() {
        if (allTaxRates.isEmpty()) {
            showFeedback("Nao existem taxas de IVA para editar.", true);
            return;
        }

        sharedModalController.prepareForForm("Editar taxa de IVA");
        showOnlyTaxRateForm();
        sharedModalController.modalTaxRateCombo.getItems().setAll(allTaxRates);
        sharedModalController.modalTaxRateCombo.getSelectionModel().selectFirst();
        fillTaxRateForm(sharedModalController.modalTaxRateCombo.getValue());
    }

    @FXML
    public void handleExportSection(ActionEvent event) {
        if (adminService == null || currentOverview == null) {
            showFeedback("Exportacao indisponivel.", true);
            return;
        }

        try {
            FinancialExportService.FinancialExportSnapshot snapshot = buildExportSnapshot();
            Path exportDirectory = resolveExportDirectory();
            String timestamp = EXPORT_TIMESTAMP_FORMAT.format(LocalDateTime.now());
            boolean exportPdf = event != null && event.getSource() == exportPdfButton;
            Path exportPath = exportDirectory.resolve(
                    "relatorio_financeiro_" + timestamp + (exportPdf ? ".pdf" : ".csv"));

            if (exportPdf) {
                exportService.exportPdf(snapshot, exportPath);
            } else {
                exportService.exportCsv(snapshot, exportPath);
            }

            showFeedback("Exportacao concluida: " + exportPath.toAbsolutePath(), false);
        } catch (Exception exception) {
            showFeedback("Falha na exportacao financeira: " + exception.getMessage(), true);
        }
    }

    @FXML
    public void handleModalCancel() {
        sharedModalController.hide();
    }

    @FXML
    public void handleModalSave() {
        try {
            AdminTaxRateDTO selected = sharedModalController.modalTaxRateCombo.getValue();
            if (selected == null || selected.id() == null) {
                sharedModalController.showError("Selecione uma taxa de IVA valida.");
                return;
            }

            BigDecimal rate = AdminFormatUtils.parseRequiredDecimal(
                    sharedModalController.modalTaxRateValueField.getText(), "Taxa de IVA");
            adminService.updateTaxRate(
                    selected.id(),
                    sharedModalController.modalTaxRateNameField.getText(),
                    rate,
                    sharedModalController.modalTaxRateDescriptionField.getText(),
                    sharedModalController.modalTaxRateActiveCheck.isSelected());

            sharedModalController.hide();
            reloadFinancialData();
            showFeedback("Taxa de IVA atualizada com sucesso.", false);
        } catch (Exception exception) {
            sharedModalController.showError("Falha ao atualizar taxa de IVA: " + exception.getMessage());
        }
    }

    @FXML
    public void handleModalConfirmDelete() {
        sharedModalController.hide();
    }

    @FXML
    public void handleCloseDetailPanel() {
        paymentsTable.getSelectionModel().clearSelection();
        hidePaymentDetailPanel();
    }

    private void reloadFinancialData() {
        if (adminService == null) {
            return;
        }
        allPayments.setAll(adminService.listPaymentsByTrip(activePeriod));
        allTaxRates.setAll(adminService.listTaxRates());
        currentOverview = adminService.getFinancialOverview(activePeriod);
        applyPaymentFilter();
        refreshUiState();
    }

    private void setPeriod(AdminFinancialPeriod period) {
        if (period == null || period == activePeriod) {
            return;
        }
        activePeriod = period;
        reloadFinancialData();
    }

    private void setPaymentStatusFilter(PaymentStatus status) {
        activePaymentStatusFilter = status;
        applyPaymentFilter();
        refreshUiState();
    }

    private void applyPaymentFilter() {
        String query = AdminFormatUtils.normalize(searchField == null ? "" : searchField.getText());
        filteredPayments.setPredicate(payment -> matchesStatus(payment) && matchesSearch(payment, query));
    }

    private boolean matchesStatus(AdminPaymentByTripDTO payment) {
        return activePaymentStatusFilter == null || payment.status() == activePaymentStatusFilter;
    }

    private boolean matchesSearch(AdminPaymentByTripDTO payment, String query) {
        if (query.isBlank()) {
            return true;
        }
        String paymentId = payment.paymentId() == null ? "" : String.valueOf(payment.paymentId());
        String tripId = payment.tripId() == null ? "" : String.valueOf(payment.tripId());
        return AdminFormatUtils.normalize(paymentId).contains(query)
                || AdminFormatUtils.normalize(tripId).contains(query)
                || AdminFormatUtils.normalize(payment.clientName()).contains(query)
                || AdminFormatUtils.normalize(payment.driverName()).contains(query)
                || AdminFormatUtils.normalize(AdminFormatUtils.prettyPaymentMethod(payment.paymentMethodType()))
                        .contains(query)
                || AdminFormatUtils.normalize(AdminFormatUtils.prettyPaymentStatus(payment.status()))
                        .contains(query);
    }

    private void configurePaymentsTableColumns() {
        paymentIdColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().paymentId() == null ? "-" : "#" + cellData.getValue().paymentId()));
        paymentTripIdColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().tripId() == null ? "-" : "#" + cellData.getValue().tripId()));
        paymentClientColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                AdminFormatUtils.fallback(cellData.getValue().clientName())));
        paymentMethodColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                AdminFormatUtils.prettyPaymentMethod(cellData.getValue().paymentMethodType())));
        paymentAmountColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                AdminFormatUtils.formatPaymentAmount(cellData.getValue())));
        paymentDateColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().paymentDate() == null ? "-"
                        : PAYMENT_DATE_FORMAT.format(cellData.getValue().paymentDate())));
        paymentStatusColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                AdminFormatUtils.prettyPaymentStatus(cellData.getValue().status())));

        paymentStatusColumn.setCellFactory(column -> new PaymentStatusTableCell());
    }

    private void refreshUiState() {
        updateFilterSummary();
        if (dashboardController != null) {
            dashboardController.update(activePeriod, currentOverview, List.copyOf(allPayments));
        }
        setPeriodButtons();
    }

    private void updateFilterSummary() {
        long pending = countPayments(PaymentStatus.PENDING);
        long processed = countPayments(PaymentStatus.PROCESSED);
        long failed = countPayments(PaymentStatus.FAILED);
        long refunded = countPayments(PaymentStatus.REFUNDED);

        setLabelText(listInfoLabel,
                "A mostrar " + filteredPayments.size() + " de " + allPayments.size() + " pagamentos");
        setLabelText(paymentsTableTitle, "Ultimos Pagamentos"
                + " | Proc: " + processed
                + " Pend: " + pending
                + " Falh: " + failed
                + " Reemb: " + refunded);
        updatePaymentFilterChips();
    }

    private long countPayments(PaymentStatus status) {
        return allPayments.stream().filter(payment -> payment.status() == status).count();
    }

    private void updatePaymentFilterChips() {
        updateChip(allPaymentsFilterButton, activePaymentStatusFilter == null);
        updateChip(processedPaymentsFilterButton, activePaymentStatusFilter == PaymentStatus.PROCESSED);
        updateChip(failedPaymentsFilterButton, activePaymentStatusFilter == PaymentStatus.FAILED);
        updateChip(refundedPaymentsFilterButton, activePaymentStatusFilter == PaymentStatus.REFUNDED);
        updateChip(pendingPaymentsFilterButton, activePaymentStatusFilter == PaymentStatus.PENDING);
    }

    private void updateChip(Button button, boolean active) {
        if (button == null) {
            return;
        }
        button.getStyleClass().removeAll("filter-chip", "filter-chip-active");
        button.getStyleClass().add(active ? "filter-chip-active" : "filter-chip");
    }

    private void setPeriodButtons() {
        updatePeriodChip(periodDayButton, activePeriod == AdminFinancialPeriod.DAY);
        updatePeriodChip(periodWeekButton, activePeriod == AdminFinancialPeriod.WEEK);
        updatePeriodChip(periodMonthButton, activePeriod == AdminFinancialPeriod.MONTH);
        updatePeriodChip(periodYearButton, activePeriod == AdminFinancialPeriod.YEAR);
        updatePeriodChip(periodAllButton, activePeriod == AdminFinancialPeriod.ALL);
    }

    private void updatePeriodChip(Button button, boolean active) {
        if (button == null) {
            return;
        }
        button.getStyleClass().removeAll("period-chip", "period-chip-active");
        button.getStyleClass().add(active ? "period-chip-active" : "period-chip");
    }

    private void showOnlyTaxRateForm() {
        AdminModalIncludeController.setVisible(sharedModalController.modalUsersFormSection, false);
        AdminModalIncludeController.setVisible(sharedModalController.modalTripsFormSection, false);
        AdminModalIncludeController.setVisible(sharedModalController.modalTaxRateFormSection, true);
        AdminModalIncludeController.setVisible(sharedModalController.modalDeleteSection, false);
    }

    private void fillTaxRateForm(AdminTaxRateDTO taxRate) {
        if (taxRate == null) {
            sharedModalController.modalTaxRateNameField.clear();
            sharedModalController.modalTaxRateValueField.clear();
            sharedModalController.modalTaxRateDescriptionField.clear();
            sharedModalController.modalTaxRateActiveCheck.setSelected(false);
            return;
        }
        sharedModalController.modalTaxRateNameField
                .setText(AdminFormatUtils.fallback(taxRate.name()).equals("-") ? "" : taxRate.name());
        sharedModalController.modalTaxRateValueField
                .setText(taxRate.rate() == null ? "" : taxRate.rate().toPlainString());
        sharedModalController.modalTaxRateDescriptionField
                .setText(AdminFormatUtils.fallback(taxRate.description()).equals("-") ? ""
                        : taxRate.description());
        sharedModalController.modalTaxRateActiveCheck.setSelected(Boolean.TRUE.equals(taxRate.active()));
    }

    private FinancialExportService.FinancialExportSnapshot buildExportSnapshot() {
        String scopeDescription = AdminFormatUtils.prettyFinancialPeriod(activePeriod)
                + " | " + filteredPayments.size() + " de " + allPayments.size() + " pagamentos";
        return new FinancialExportService.FinancialExportSnapshot(
                LocalDateTime.now(),
                scopeDescription,
                currentOverview,
                List.copyOf(allPayments),
                List.copyOf(allTaxRates),
                FinancialDashboardController.groupPaymentMethods(List.copyOf(allPayments)));
    }

    private Path resolveExportDirectory() throws Exception {
        Path exportDirectory = Path.of(System.getProperty("user.home"), "Documents", "obar-exports");
        Files.createDirectories(exportDirectory);
        return exportDirectory;
    }

    private void showFeedback(String message, boolean isError) {
        if (feedbackLabel == null) {
            return;
        }
        feedbackLabel.setText(message == null ? "" : message);
        feedbackLabel.getStyleClass().removeAll("feedback-success", "feedback-error");
        feedbackLabel.getStyleClass().add(isError ? "feedback-error" : "feedback-success");
    }

    private void showPaymentDetails(AdminPaymentByTripDTO payment) {
        if (payment == null) {
            hidePaymentDetailPanel();
            return;
        }

        String taxRate = payment.taxRateApplied() == null ? "-"
                : payment.taxRateApplied().multiply(new BigDecimal("100"))
                        .setScale(2, RoundingMode.HALF_UP) + "%";
        setLabelText(detailInitialsLabel, payment.paymentId() == null ? "--" : "#" + payment.paymentId());
        setLabelText(detailTitleLabel, "Detalhe do pagamento");
        setLabelText(detailNameLabel, AdminFormatUtils.formatPaymentAmount(payment));
        setLabelText(detailEmailLabel, "Cliente: " + AdminFormatUtils.fallback(payment.clientName()));
        setLabelText(detailStatusLabel, AdminFormatUtils.prettyPaymentStatus(payment.status()));
        setLabelText(detailRoleLabel, AdminFormatUtils.prettyPaymentMethod(payment.paymentMethodType()));
        setLabelText(detailPhoneValueLabel,
                "Metodo: " + AdminFormatUtils.prettyPaymentMethod(payment.paymentMethodType()));
        setLabelText(detailCreatedValueLabel,
                payment.paymentDate() == null ? "-" : PAYMENT_DATE_FORMAT.format(payment.paymentDate()));
        setLabelText(detailCardOneTitleLabel, "ID Viagem");
        setLabelText(detailCardOneValueLabel, payment.tripId() == null ? "-" : "#" + payment.tripId());
        setLabelText(detailCardTwoTitleLabel, "Valor");
        setLabelText(detailCardTwoValueLabel, AdminFormatUtils.formatPaymentAmount(payment));
        setLabelText(detailCardThreeTitleLabel, "Estado");
        setLabelText(detailCardThreeValueLabel, AdminFormatUtils.prettyPaymentStatus(payment.status()));
        setLabelText(detailCardFourTitleLabel, "Moeda");
        setLabelText(detailCardFourValueLabel, AdminFormatUtils.fallback(payment.currencyCode()));
        setLabelText(detailReferenceTitleLabel, "Taxa aplicada");
        setLabelText(detailReferenceValueLabel, taxRate);
        setLabelText(detailExtraOneTitleLabel, "Motorista");
        setLabelText(detailExtraOneValueLabel, AdminFormatUtils.fallback(payment.driverName()));
        setLabelText(detailExtraTwoTitleLabel, "Data pagamento");
        setLabelText(detailExtraTwoValueLabel,
                payment.paymentDate() == null ? "-" : PAYMENT_DATE_FORMAT.format(payment.paymentDate()));
        setLabelText(detailExtraThreeTitleLabel, "Periodo");
        setLabelText(detailExtraThreeValueLabel, AdminFormatUtils.prettyFinancialPeriod(activePeriod));
        AdminModalIncludeController.setVisible(detailPanel, true);
    }

    private void hidePaymentDetailPanel() {
        AdminModalIncludeController.setVisible(detailPanel, false);
    }

    private void setLabelText(Label label, String text) {
        if (label != null) {
            label.setText(text == null ? "-" : text);
        }
    }

    /** Renders IVA/tax rate values in the tax-rate combo box. */
    private static final class TaxRateListCell extends ListCell<AdminTaxRateDTO> {
        @Override
        protected void updateItem(AdminTaxRateDTO item, boolean empty) {
            super.updateItem(item, empty);
            setText(empty || item == null ? null : AdminFormatUtils.formatTaxRateDisplay(item));
        }
    }

    /** Converts IVA/tax rate DTOs to the text shown by the combo box. */
    private static final class TaxRateStringConverter extends StringConverter<AdminTaxRateDTO> {
        @Override
        public String toString(AdminTaxRateDTO taxRate) {
            return taxRate == null ? "" : AdminFormatUtils.formatTaxRateDisplay(taxRate);
        }

        @Override
        public AdminTaxRateDTO fromString(String value) {
            return null;
        }
    }

    /** Applies payment status styles to rows in the payments table. */
    private static final class PaymentStatusTableCell extends TableCell<AdminPaymentByTripDTO, String> {
        @Override
        protected void updateItem(String statusLabel, boolean empty) {
            super.updateItem(statusLabel, empty);
            getStyleClass().removeAll("payment-processed", "payment-pending", "payment-failed", "payment-refunded");
            if (empty || statusLabel == null) {
                setText(null);
                return;
            }

            setText(statusLabel);
            AdminPaymentByTripDTO payment = getIndex() >= 0 && getIndex() < getTableView().getItems().size()
                    ? getTableView().getItems().get(getIndex())
                    : null;
            if (payment == null || payment.status() == null) {
                return;
            }

            switch (payment.status()) {
                case PROCESSED -> getStyleClass().add("payment-processed");
                case PENDING -> getStyleClass().add("payment-pending");
                case FAILED -> getStyleClass().add("payment-failed");
                case REFUNDED -> getStyleClass().add("payment-refunded");
            }
        }
    }
}
