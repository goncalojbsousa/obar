package com.obar.desktop.admin.sections.financial;

import com.obar.bll.admin.AdminFinancialPeriod;
import com.obar.bll.admin.AdminPaymentByTripDTO;
import com.obar.bll.admin.AdminService;
import com.obar.desktop.admin.sections.AdminSectionController;
import com.obar.desktop.admin.shared.AdminFormatUtils;
import com.obar.desktop.admin.shared.AdminModalController;
import com.obar.desktop.admin.shared.AdminModalIncludeController;
import com.obar.desktop.admin.shared.DetailPanelBinder;
import com.obar.model.enums.PaymentStatus;
import javafx.beans.property.SimpleStringProperty;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Controller for the Financial admin section.
 *
 * <p>
 * Responsibilities:
 * - wire FXML controls with ViewModel properties
 * - dispatch events to ViewModel/Presenters
 * - refresh UI when observable state changes
 */
public class FinancialController implements AdminSectionController {

    private static final DateTimeFormatter PAYMENT_DATE_FORMAT = DateTimeFormatter.ofPattern(
            "dd/MM/yyyy HH:mm", Locale.forLanguageTag("pt-PT"));
    private static final DateTimeFormatter EXPORT_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern(
            "yyyyMMdd_HHmmss", Locale.ROOT);

    private final FinancialExportService exportService = new FinancialExportService();
    private final FinancialDashboardPresenter dashboardPresenter = new FinancialDashboardPresenter();

    private FinancialViewModel viewModel;
    private FinancialModalPresenter modalPresenter;
    private DetailPanelBinder detailBinder;
    private AdminService pendingAdminService;

    // - table & toolbar -
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

    // - detail panel labels (wired into DetailPanelBinder) -
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

    // - modal fields (bound from shared modal) -
    @FXML
    private AdminModalIncludeController sharedModalController;

    @Override
    public void setAdminService(AdminService adminService) {
        pendingAdminService = adminService;
        if (viewModel != null) {
            viewModel.init(adminService);
            viewModel.reload();
            refreshUiState();
        }
    }

    @FXML
    public void initialize() {
        viewModel = new FinancialViewModel();
        if (pendingAdminService != null) {
            viewModel.init(pendingAdminService);
        }

        AdminModalController modalController = sharedModalController.createModalController();
        sharedModalController.bindActions(this::handleModalCancel, this::handleModalSave,
                this::handleModalConfirmDelete);

        modalPresenter = new FinancialModalPresenter(viewModel, modalController);
        modalPresenter.wireFields(
                sharedModalController.getModalTaxRateFormSection(),
                sharedModalController.getModalDeleteMessageLabel(),
                sharedModalController.getModalTaxRateCombo(),
                sharedModalController.getModalTaxRateNameField(),
                sharedModalController.getModalTaxRateValueField(),
                sharedModalController.getModalTaxRateDescriptionField(),
                sharedModalController.getModalTaxRateActiveCheck());
        modalPresenter.initialize();

        detailBinder = new DetailPanelBinder(
                detailPanel,
                detailInitialsLabel, detailTitleLabel, detailNameLabel, detailEmailLabel,
                detailStatusLabel, detailRoleLabel, detailPhoneValueLabel, detailCreatedValueLabel,
                detailCardOneTitleLabel, detailCardOneValueLabel,
                detailCardTwoTitleLabel, detailCardTwoValueLabel,
                detailCardThreeTitleLabel, detailCardThreeValueLabel,
                detailCardFourTitleLabel, detailCardFourValueLabel,
                detailReferenceTitleLabel, detailReferenceValueLabel,
                detailExtraOneTitleLabel, detailExtraOneValueLabel,
                detailExtraTwoTitleLabel, detailExtraTwoValueLabel,
                detailExtraThreeTitleLabel, detailExtraThreeValueLabel);

        paymentsTable.setItems(viewModel.getFilteredPayments());
        searchField.textProperty().bindBidirectional(viewModel.searchQueryProperty());

        paymentsTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, previous, current) -> onPaymentSelectionChanged(current));
        viewModel.getAllPayments()
                .addListener((javafx.collections.ListChangeListener<AdminPaymentByTripDTO>) change -> {
                    refreshUiState();
                });
        viewModel.activePaymentStatusFilterProperty().addListener((obs, oldValue, newValue) -> refreshUiState());
        viewModel.activePeriodProperty().addListener((obs, oldValue, newValue) -> refreshUiState());

        setupColumns();
        detailBinder.setVisible(false);
    }

    @Override
    public void onSectionActivated() {
        if (viewModel != null) {
            viewModel.reload();
            refreshUiState();
            detailBinder.setVisible(false);
        }
    }

    // - period handlers -

    @FXML
    public void handlePeriodChanged() {
        if (viewModel != null) {
            viewModel.reload();
            refreshUiState();
        }
    }

    @FXML
    public void handleFinancialPeriodDay() {
        viewModel.setPeriod(AdminFinancialPeriod.DAY);
    }

    @FXML
    public void handleFinancialPeriodWeek() {
        viewModel.setPeriod(AdminFinancialPeriod.WEEK);
    }

    @FXML
    public void handleFinancialPeriodMonth() {
        viewModel.setPeriod(AdminFinancialPeriod.MONTH);
    }

    @FXML
    public void handleFinancialPeriodYear() {
        viewModel.setPeriod(AdminFinancialPeriod.YEAR);
    }

    @FXML
    public void handleFinancialPeriodAll() {
        viewModel.setPeriod(AdminFinancialPeriod.ALL);
    }

    // - filter handlers -

    @FXML
    public void handleFilterAll() {
        viewModel.setPaymentStatusFilter(null);
    }

    @FXML
    public void handleFilterActive() {
        viewModel.setPaymentStatusFilter(PaymentStatus.PROCESSED);
    }

    @FXML
    public void handleFilterInactive() {
        viewModel.setPaymentStatusFilter(PaymentStatus.FAILED);
    }

    @FXML
    public void handleFilterBlocked() {
        viewModel.setPaymentStatusFilter(PaymentStatus.REFUNDED);
    }

    @FXML
    public void handleFilterPending() {
        viewModel.setPaymentStatusFilter(PaymentStatus.PENDING);
    }

    @FXML
    public void handleEditTaxRate() {
        FinancialModalPresenter.PersistResult result = modalPresenter.openEditTaxRate();
        if (!result.success()) {
            showFeedback(result.message(), true);
        }
    }

    @FXML
    public void handleExportSection(ActionEvent event) {
        try {
            if (viewModel == null || viewModel.getAdminService() == null) {
                return;
            }
            FinancialExportService.FinancialExportSnapshot snapshot = buildExportSnapshot();
            Path exportDir = resolveExportDirectory();
            String timestamp = EXPORT_TIMESTAMP_FORMAT.format(LocalDateTime.now());
            boolean exportPdf = event != null && event.getSource() == exportPdfButton;
            Path exportPath = exportDir.resolve("relatorio_financeiro_" + timestamp + (exportPdf ? ".pdf" : ".csv"));
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

    // - modal handlers -

    @FXML
    public void handleModalCancel() {
        modalPresenter.close();
    }

    @FXML
    public void handleModalSave() {
        FinancialModalPresenter.PersistResult result = modalPresenter.saveTaxRate();
        if (result.success()) {
            modalPresenter.close();
            viewModel.reload();
            refreshUiState();
            showFeedback(result.message(), false);
        } else if (!result.silent() && result.message() != null) {
            showFeedback(result.message(), true);
        }
    }

    @FXML
    public void handleModalConfirmDelete() {
        modalPresenter.close();
    }

    @FXML
    public void handleCloseDetailPanel() {
        paymentsTable.getSelectionModel().clearSelection();
        detailBinder.setVisible(false);
    }

    // - private helpers -

    private void onPaymentSelectionChanged(AdminPaymentByTripDTO payment) {
        if (payment == null) {
            detailBinder.setVisible(false);
            return;
        }
        detailBinder.bind(PaymentDetailMapper.fromPayment(payment, viewModel));
    }

    private void setupColumns() {
        paymentIdColumn.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getPaymentId() == null ? "-" : "#" + cd.getValue().getPaymentId()));
        paymentTripIdColumn.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getTripId() == null ? "-" : "#" + cd.getValue().getTripId()));
        paymentClientColumn.setCellValueFactory(
                cd -> new SimpleStringProperty(AdminFormatUtils.fallback(cd.getValue().getClientName())));
        paymentMethodColumn.setCellValueFactory(cd -> new SimpleStringProperty(
                AdminFormatUtils.prettyPaymentMethod(cd.getValue().getPaymentMethodType())));
        paymentAmountColumn.setCellValueFactory(
                cd -> new SimpleStringProperty(AdminFormatUtils.formatPaymentAmount(cd.getValue())));
        paymentDateColumn.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getPaymentDate() == null ? "-"
                        : PAYMENT_DATE_FORMAT.format(cd.getValue().getPaymentDate())));
        paymentStatusColumn.setCellValueFactory(
                cd -> new SimpleStringProperty(AdminFormatUtils.prettyPaymentStatus(cd.getValue().getStatus())));
        paymentStatusColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("payment-processed", "payment-pending", "payment-failed", "payment-refunded");
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                setText(item);
                if (getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    return;
                }
                AdminPaymentByTripDTO row = getTableView().getItems().get(getIndex());
                if (row.getStatus() == null) {
                    return;
                }
                switch (row.getStatus()) {
                    case PROCESSED -> getStyleClass().add("payment-processed");
                    case PENDING -> getStyleClass().add("payment-pending");
                    case FAILED -> getStyleClass().add("payment-failed");
                    case REFUNDED -> getStyleClass().add("payment-refunded");
                }
            }
        });
    }

    private void refreshUiState() {
        dashboardPresenter.updateFilterSummary(viewModel, paymentsTableTitle, listInfoLabel);
        dashboardPresenter.updateDashboard(
                viewModel,
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

        periodDayButton.setDisable(viewModel.getActivePeriod() == AdminFinancialPeriod.DAY);
        periodWeekButton.setDisable(viewModel.getActivePeriod() == AdminFinancialPeriod.WEEK);
        periodMonthButton.setDisable(viewModel.getActivePeriod() == AdminFinancialPeriod.MONTH);
        periodYearButton.setDisable(viewModel.getActivePeriod() == AdminFinancialPeriod.YEAR);
        periodAllButton.setDisable(viewModel.getActivePeriod() == AdminFinancialPeriod.ALL);
    }

    private FinancialExportService.FinancialExportSnapshot buildExportSnapshot() {
        String scope = AdminFormatUtils.prettyFinancialPeriod(viewModel.getActivePeriod())
                + " | " + viewModel.getFilteredPayments().size() + " de " + viewModel.getAllPayments().size()
                + " pagamentos";
        return new FinancialExportService.FinancialExportSnapshot(
                LocalDateTime.now(),
                scope,
                viewModel.getCurrentOverview(),
                viewModel.getPaymentsSnapshot(),
                viewModel.getTaxRatesSnapshot(),
                viewModel.groupPaymentMethods());
    }

    private Path resolveExportDirectory() throws Exception {
        Path dir = Path.of(System.getProperty("user.home"), "Documents", "obar-exports");
        Files.createDirectories(dir);
        return dir;
    }

    private void showFeedback(String message, boolean isError) {
        if (feedbackLabel == null) {
            return;
        }
        feedbackLabel.setText(message);
        feedbackLabel.getStyleClass().removeAll("feedback-success", "feedback-error");
        feedbackLabel.getStyleClass().add(isError ? "feedback-error" : "feedback-success");
    }
}
