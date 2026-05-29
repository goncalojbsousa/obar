package com.obar.desktop.admin.sections.financial;

import com.obar.bll.admin.AdminFinancialOverviewDTO;
import com.obar.bll.admin.AdminFinancialPeriod;
import com.obar.bll.admin.AdminPaymentByTripDTO;
import com.obar.bll.admin.AdminService;
import com.obar.bll.admin.AdminTaxRateCommand;
import com.obar.bll.admin.AdminTaxRateDTO;
import com.obar.desktop.admin.sections.AdminSectionController;
import com.obar.desktop.admin.shared.AdminFormatUtils;
import com.obar.desktop.admin.shared.AdminModalIncludeController;
import com.obar.desktop.admin.shared.AdminParseUtils;
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
    private FinancialPaymentDetailPanel paymentDetailPanel;

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
        configurePaymentDetailPanel();

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

    private void configurePaymentDetailPanel() {
        paymentDetailPanel = new FinancialPaymentDetailPanel(
                detailPanel,
                detailInitialsLabel,
                detailTitleLabel,
                detailNameLabel,
                detailEmailLabel,
                detailStatusLabel,
                detailRoleLabel,
                detailPhoneValueLabel,
                detailCreatedValueLabel,
                detailCardOneTitleLabel,
                detailCardOneValueLabel,
                detailCardTwoTitleLabel,
                detailCardTwoValueLabel,
                detailCardThreeTitleLabel,
                detailCardThreeValueLabel,
                detailCardFourTitleLabel,
                detailCardFourValueLabel,
                detailReferenceTitleLabel,
                detailReferenceValueLabel,
                detailExtraOneTitleLabel,
                detailExtraOneValueLabel,
                detailExtraTwoTitleLabel,
                detailExtraTwoValueLabel,
                detailExtraThreeTitleLabel,
                detailExtraThreeValueLabel);
    }

    private void configureTaxRateModal() {
        sharedModalController.bindActions(this::handleModalCancel, this::handleModalSave,
                this::handleModalConfirmDelete);
        var taxRateCombo = sharedModalController.<AdminTaxRateDTO>getModalTaxRateCombo();
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
    public void handlePeriodChanged() {
        reloadFinancialData();
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
        sharedModalController.<AdminTaxRateDTO>getModalTaxRateCombo().getItems().setAll(allTaxRates);
        sharedModalController.<AdminTaxRateDTO>getModalTaxRateCombo().getSelectionModel().selectFirst();
        fillTaxRateForm(sharedModalController.<AdminTaxRateDTO>getModalTaxRateCombo().getValue());
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
            AdminTaxRateDTO selected = sharedModalController.<AdminTaxRateDTO>getModalTaxRateCombo().getValue();
            if (selected == null || selected.getId() == null) {
                sharedModalController.showError("Selecione uma taxa de IVA valida.");
                return;
            }

            BigDecimal rate = AdminParseUtils.parseRequiredDecimal(
                    sharedModalController.getModalTaxRateValueField().getText(), "Taxa de IVA");
            adminService.updateTaxRate(selected.getId(), new AdminTaxRateCommand(
                    sharedModalController.getModalTaxRateNameField().getText(),
                    rate,
                    sharedModalController.getModalTaxRateDescriptionField().getText(),
                    sharedModalController.getModalTaxRateActiveCheck().isSelected()));

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
        return activePaymentStatusFilter == null || payment.getStatus() == activePaymentStatusFilter;
    }

    private boolean matchesSearch(AdminPaymentByTripDTO payment, String query) {
        if (query.isBlank()) {
            return true;
        }
        String paymentId = payment.getPaymentId() == null ? "" : String.valueOf(payment.getPaymentId());
        String tripId = payment.getTripId() == null ? "" : String.valueOf(payment.getTripId());
        return AdminFormatUtils.normalize(paymentId).contains(query)
                || AdminFormatUtils.normalize(tripId).contains(query)
                || AdminFormatUtils.normalize(payment.getClientName()).contains(query)
                || AdminFormatUtils.normalize(payment.getDriverName()).contains(query)
                || AdminFormatUtils.normalize(AdminFormatUtils.prettyPaymentMethod(payment.getPaymentMethodType()))
                        .contains(query)
                || AdminFormatUtils.normalize(AdminFormatUtils.prettyPaymentStatus(payment.getStatus()))
                        .contains(query);
    }

    private void configurePaymentsTableColumns() {
        paymentIdColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getPaymentId() == null ? "-" : "#" + cellData.getValue().getPaymentId()));
        paymentTripIdColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getTripId() == null ? "-" : "#" + cellData.getValue().getTripId()));
        paymentClientColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                AdminFormatUtils.fallback(cellData.getValue().getClientName())));
        paymentMethodColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                AdminFormatUtils.prettyPaymentMethod(cellData.getValue().getPaymentMethodType())));
        paymentAmountColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                AdminFormatUtils.formatPaymentAmount(cellData.getValue())));
        paymentDateColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getPaymentDate() == null ? "-"
                        : PAYMENT_DATE_FORMAT.format(cellData.getValue().getPaymentDate())));
        paymentStatusColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                AdminFormatUtils.prettyPaymentStatus(cellData.getValue().getStatus())));

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
    }

    private long countPayments(PaymentStatus status) {
        return allPayments.stream().filter(payment -> payment.getStatus() == status).count();
    }

    private void setPeriodButtons() {
        if (periodDayButton != null)
            periodDayButton.setDisable(activePeriod == AdminFinancialPeriod.DAY);
        if (periodWeekButton != null)
            periodWeekButton.setDisable(activePeriod == AdminFinancialPeriod.WEEK);
        if (periodMonthButton != null)
            periodMonthButton.setDisable(activePeriod == AdminFinancialPeriod.MONTH);
        if (periodYearButton != null)
            periodYearButton.setDisable(activePeriod == AdminFinancialPeriod.YEAR);
        if (periodAllButton != null)
            periodAllButton.setDisable(activePeriod == AdminFinancialPeriod.ALL);
    }

    private void showOnlyTaxRateForm() {
        AdminModalIncludeController.setVisible(sharedModalController.getModalUsersFormSection(), false);
        AdminModalIncludeController.setVisible(sharedModalController.getModalTripsFormSection(), false);
        AdminModalIncludeController.setVisible(sharedModalController.getModalTaxRateFormSection(), true);
        AdminModalIncludeController.setVisible(sharedModalController.getModalDeleteSection(), false);
    }

    private void fillTaxRateForm(AdminTaxRateDTO taxRate) {
        if (taxRate == null) {
            sharedModalController.getModalTaxRateNameField().clear();
            sharedModalController.getModalTaxRateValueField().clear();
            sharedModalController.getModalTaxRateDescriptionField().clear();
            sharedModalController.getModalTaxRateActiveCheck().setSelected(false);
            return;
        }
        sharedModalController.getModalTaxRateNameField()
                .setText(AdminFormatUtils.fallback(taxRate.getName()).equals("-") ? "" : taxRate.getName());
        sharedModalController.getModalTaxRateValueField()
                .setText(taxRate.getRate() == null ? "" : taxRate.getRate().toPlainString());
        sharedModalController.getModalTaxRateDescriptionField()
                .setText(AdminFormatUtils.fallback(taxRate.getDescription()).equals("-") ? ""
                        : taxRate.getDescription());
        sharedModalController.getModalTaxRateActiveCheck().setSelected(Boolean.TRUE.equals(taxRate.getActive()));
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
        if (paymentDetailPanel != null) {
            paymentDetailPanel.show(payment, activePeriod);
        }
    }

    private void hidePaymentDetailPanel() {
        if (paymentDetailPanel != null) {
            paymentDetailPanel.hide();
        } else if (detailPanel != null) {
            detailPanel.setVisible(false);
            detailPanel.setManaged(false);
        }
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
            if (payment == null || payment.getStatus() == null) {
                return;
            }

            switch (payment.getStatus()) {
                case PROCESSED -> getStyleClass().add("payment-processed");
                case PENDING -> getStyleClass().add("payment-pending");
                case FAILED -> getStyleClass().add("payment-failed");
                case REFUNDED -> getStyleClass().add("payment-refunded");
            }
        }
    }
}
