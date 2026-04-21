package com.obar.desktop.admin.sections.financial;

import com.lowagie.text.DocumentException;
import com.obar.bll.admin.AdminFinancialOverviewDTO;
import com.obar.bll.admin.AdminFinancialPeriod;
import com.obar.bll.admin.AdminPaymentByTripDTO;
import com.obar.bll.admin.AdminService;
import com.obar.bll.admin.AdminTaxRateCommand;
import com.obar.bll.admin.AdminTaxRateDTO;
import com.obar.desktop.admin.sections.AdminSectionController;
import com.obar.desktop.admin.shared.AdminFormatUtils;
import com.obar.desktop.admin.shared.AdminModalController;
import com.obar.desktop.admin.shared.AdminModalIncludeController;
import com.obar.model.enums.PaymentStatus;
import javafx.event.ActionEvent;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
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

public class FinancialController implements AdminSectionController {

    private static final DateTimeFormatter PAYMENT_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.forLanguageTag("pt-PT"));
    private static final DateTimeFormatter DAY_LABEL_FORMAT = DateTimeFormatter.ofPattern("dd/MM", Locale.forLanguageTag("pt-PT"));
    private static final DateTimeFormatter MONTH_LABEL_FORMAT = DateTimeFormatter.ofPattern("MMM", Locale.forLanguageTag("pt-PT"));
    private static final DateTimeFormatter EXPORT_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss", Locale.ROOT);

    private enum RevenueBucketType {
        DAY, WEEK, MONTH, YEAR, ALL
    }

    private record RevenueBucket(String label, String tooltip, BigDecimal value) {
    }

    private final ObservableList<AdminPaymentByTripDTO> allPayments = FXCollections.observableArrayList();
    private final FilteredList<AdminPaymentByTripDTO> filteredPayments = new FilteredList<>(allPayments, payment -> true);
    private final ObservableList<AdminTaxRateDTO> allTaxRates = FXCollections.observableArrayList();
    private final FinancialExportService exportService = new FinancialExportService();

    private AdminService adminService;
    private AdminFinancialPeriod currentFinancialPeriod = AdminFinancialPeriod.MONTH;
    private AdminFinancialOverviewDTO currentFinancialOverview;
    private PaymentStatus currentPaymentStatusFilter;
    private RevenueBucketType revenueBucketType = RevenueBucketType.MONTH;
    private AdminModalController modalController;

    @FXML private VBox root;
    @FXML private TextField searchField;
    @FXML private TableView<AdminPaymentByTripDTO> paymentsTable;
    @FXML private TableColumn<AdminPaymentByTripDTO, String> paymentIdColumn;
    @FXML private TableColumn<AdminPaymentByTripDTO, String> paymentTripIdColumn;
    @FXML private TableColumn<AdminPaymentByTripDTO, String> paymentClientColumn;
    @FXML private TableColumn<AdminPaymentByTripDTO, String> paymentMethodColumn;
    @FXML private TableColumn<AdminPaymentByTripDTO, String> paymentAmountColumn;
    @FXML private TableColumn<AdminPaymentByTripDTO, String> paymentStatusColumn;
    @FXML private TableColumn<AdminPaymentByTripDTO, String> paymentDateColumn;
    @FXML private Label paymentsTableTitle;
    @FXML private Label feedbackLabel;
    @FXML private AdminModalIncludeController sharedModalController;
    @FXML private Button exportPdfButton;
    @FXML private Button exportCsvButton;
    @FXML private Button periodDayButton;
    @FXML private Button periodWeekButton;
    @FXML private Button periodMonthButton;
    @FXML private Button periodYearButton;
    @FXML private Button periodAllButton;
    @FXML private Label revenuePeriodLabel;
    @FXML private Label revenueTotalLabel;
    @FXML private Label revenueTrendLabel;
    @FXML private Label netRevenueValueLabel;
    @FXML private Label platformCommissionValueLabel;
    @FXML private Label billedTripsValueLabel;
    @FXML private Label avgTicketValueLabel;
    @FXML private Label processedPaymentsValueLabel;
    @FXML private Label refundedPaymentsValueLabel;
    @FXML private Label failedRateValueLabel;
    @FXML private Label paidDriversValueLabel;
    @FXML private HBox dailyBarsContainer;
    @FXML private Label methodOneLabel;
    @FXML private Label methodOnePercentLabel;
    @FXML private Label methodTwoLabel;
    @FXML private Label methodTwoPercentLabel;
    @FXML private Label methodThreeLabel;
    @FXML private Label methodThreePercentLabel;
    @FXML private Label methodFourLabel;
    @FXML private Label methodFourPercentLabel;
    @FXML private Label listInfoLabel;
    @FXML private Label detailInitialsLabel;
    @FXML private Label detailTitleLabel;
    @FXML private Label detailNameLabel;
    @FXML private Label detailEmailLabel;
    @FXML private Label detailStatusLabel;
    @FXML private Label detailRoleLabel;
    @FXML private Label detailCardOneTitleLabel;
    @FXML private Label detailCardOneValueLabel;
    @FXML private Label detailCardTwoTitleLabel;
    @FXML private Label detailCardTwoValueLabel;
    @FXML private Label detailCardThreeTitleLabel;
    @FXML private Label detailCardThreeValueLabel;
    @FXML private Label detailCardFourTitleLabel;
    @FXML private Label detailCardFourValueLabel;
    @FXML private Label detailReferenceTitleLabel;
    @FXML private Label detailReferenceValueLabel;
    @FXML private Label detailPhoneValueLabel;
    @FXML private Label detailCreatedValueLabel;
    @FXML private Label detailExtraOneTitleLabel;
    @FXML private Label detailExtraOneValueLabel;
    @FXML private Label detailExtraTwoTitleLabel;
    @FXML private Label detailExtraTwoValueLabel;
    @FXML private Label detailExtraThreeTitleLabel;
    @FXML private Label detailExtraThreeValueLabel;
    @FXML private VBox detailPanel;
    @FXML private VBox modalTaxRateFormSection;
    @FXML private Label modalDeleteMessageLabel;
    @FXML private ComboBox<AdminFinancialPeriod> periodComboBox;
    @FXML private ComboBox<AdminTaxRateDTO> modalTaxRateCombo;
    @FXML private TextField modalTaxRateNameField;
    @FXML private TextField modalTaxRateValueField;
    @FXML private TextField modalTaxRateDescriptionField;
    @FXML private CheckBox modalTaxRateActiveCheck;

    @Override
    public void setAdminService(AdminService adminService) {
        this.adminService = adminService;
    }

    @FXML
    public void initialize() {
        bindModalFields();
        modalController = sharedModalController.createModalController();
        sharedModalController.bindActions(this::handleModalCancel, this::handleModalSave, this::handleModalConfirmDelete);
        paymentsTable.setItems(filteredPayments);
        periodComboBox.setItems(FXCollections.observableArrayList(AdminFinancialPeriod.values()));
        periodComboBox.setValue(currentFinancialPeriod);
        modalTaxRateCombo.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(AdminTaxRateDTO item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : AdminFormatUtils.formatTaxRateDisplay(item));
            }
        });
        modalTaxRateCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(AdminTaxRateDTO item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : AdminFormatUtils.formatTaxRateDisplay(item));
            }
        });
        modalTaxRateCombo.valueProperty().addListener((obs, oldValue, newValue) -> populateTaxRateFields(newValue));
        searchField.textProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        paymentsTable.getSelectionModel().selectedItemProperty().addListener((obs, previous, current) -> updatePaymentDetailsPanel(current));
        setupColumns();
        refreshSectionData();
    }

    private void bindModalFields() {
        if (sharedModalController == null) {
            throw new IllegalStateException("Shared modal controller was not injected.");
        }
        modalTaxRateFormSection = sharedModalController.getModalTaxRateFormSection();
        modalDeleteMessageLabel = sharedModalController.getModalDeleteMessageLabel();
        modalTaxRateCombo = sharedModalController.getModalTaxRateCombo();
        modalTaxRateNameField = sharedModalController.getModalTaxRateNameField();
        modalTaxRateValueField = sharedModalController.getModalTaxRateValueField();
        modalTaxRateDescriptionField = sharedModalController.getModalTaxRateDescriptionField();
        modalTaxRateActiveCheck = sharedModalController.getModalTaxRateActiveCheck();
    }

    @Override
    public void onSectionActivated() {
        refreshSectionData();
    }

    @FXML public void handlePeriodChanged() { currentFinancialPeriod = periodComboBox.getValue() == null ? AdminFinancialPeriod.MONTH : periodComboBox.getValue(); revenueBucketType = toBucketType(currentFinancialPeriod); refreshSectionData(); }
    @FXML public void handleFinancialPeriodDay() { setFinancialPeriod(AdminFinancialPeriod.DAY); }
    @FXML public void handleFinancialPeriodWeek() { setFinancialPeriod(AdminFinancialPeriod.WEEK); }
    @FXML public void handleFinancialPeriodMonth() { setFinancialPeriod(AdminFinancialPeriod.MONTH); }
    @FXML public void handleFinancialPeriodYear() { setFinancialPeriod(AdminFinancialPeriod.YEAR); }
    @FXML public void handleFinancialPeriodAll() { setFinancialPeriod(AdminFinancialPeriod.ALL); }

    @FXML public void handleFilterAll() { setPaymentStatusFilter(null); }
    @FXML public void handleFilterActive() { setPaymentStatusFilter(PaymentStatus.PROCESSED); }
    @FXML public void handleFilterInactive() { setPaymentStatusFilter(PaymentStatus.FAILED); }
    @FXML public void handleFilterBlocked() { setPaymentStatusFilter(PaymentStatus.REFUNDED); }
    @FXML public void handleFilterPending() { setPaymentStatusFilter(PaymentStatus.PENDING); }
    @FXML public void handleEditTaxRate() { openTaxRateEditModal(); }

    @FXML
    public void handleExportSection(ActionEvent event) {
        try {
            if (adminService == null) {
                return;
            }
            FinancialExportService.FinancialExportSnapshot snapshot = buildFinancialExportSnapshot();
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

    @FXML public void handleModalCancel() { hideModal(); }
    @FXML public void handleModalSave() { persistTaxRateUpdate(); }
    @FXML public void handleModalConfirmDelete() { hideModal(); }
    @FXML public void handleCloseDetailPanel() {
        paymentsTable.getSelectionModel().clearSelection();
        clearDetails();
        setDetailsVisible(false);
    }

    private void refreshSectionData() {
        if (adminService == null) {
            return;
        }
        allPayments.setAll(adminService.listPaymentsByTrip(currentFinancialPeriod));
        allTaxRates.setAll(adminService.listTaxRates());
        currentFinancialOverview = adminService.getFinancialOverview(currentFinancialPeriod);
        applyFilters();
        updateFilterLabels();
        updateFinancialDashboard();
        updateFinancialDetailPanel();
        if (modalTaxRateCombo != null && modalTaxRateCombo.getItems().isEmpty()) {
            modalTaxRateCombo.setItems(FXCollections.observableArrayList(allTaxRates));
        }
    }

    private void setupColumns() {
        paymentIdColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getPaymentId() == null ? "-" : "#" + cellData.getValue().getPaymentId()));
        paymentTripIdColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getTripId() == null ? "-" : "#" + cellData.getValue().getTripId()));
        paymentClientColumn.setCellValueFactory(cellData -> new SimpleStringProperty(AdminFormatUtils.fallback(cellData.getValue().getClientName())));
        paymentMethodColumn.setCellValueFactory(cellData -> new SimpleStringProperty(AdminFormatUtils.prettyPaymentMethod(cellData.getValue().getPaymentMethodType())));
        paymentAmountColumn.setCellValueFactory(cellData -> new SimpleStringProperty(AdminFormatUtils.formatPaymentAmount(cellData.getValue())));
        paymentDateColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getPaymentDate() == null ? "-" : PAYMENT_DATE_FORMAT.format(cellData.getValue().getPaymentDate())));
        paymentStatusColumn.setCellValueFactory(cellData -> new SimpleStringProperty(AdminFormatUtils.prettyPaymentStatus(cellData.getValue().getStatus())));
        paymentStatusColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                setText(item);
            }
        });
    }

    private void applyFilters() {
        String query = AdminFormatUtils.normalize(searchField.getText());
        filteredPayments.setPredicate(payment -> matchesPaymentStatus(payment) && matchesPaymentQuery(payment, query));
        updateFilterLabels();
    }

    private boolean matchesPaymentStatus(AdminPaymentByTripDTO payment) {
        return currentPaymentStatusFilter == null || payment.getStatus() == currentPaymentStatusFilter;
    }

    private boolean matchesPaymentQuery(AdminPaymentByTripDTO payment, String query) {
        if (query.isBlank()) {
            return true;
        }
        String paymentIdValue = payment.getPaymentId() == null ? "" : String.valueOf(payment.getPaymentId());
        String tripIdValue = payment.getTripId() == null ? "" : String.valueOf(payment.getTripId());
        return AdminFormatUtils.normalize(paymentIdValue).contains(query)
                || AdminFormatUtils.normalize(tripIdValue).contains(query)
                || AdminFormatUtils.normalize(payment.getClientName()).contains(query)
                || AdminFormatUtils.normalize(payment.getDriverName()).contains(query)
                || AdminFormatUtils.normalize(AdminFormatUtils.prettyPaymentMethod(payment.getPaymentMethodType())).contains(query)
                || AdminFormatUtils.normalize(AdminFormatUtils.prettyPaymentStatus(payment.getStatus())).contains(query);
    }

    private void updateFinancialDashboard() {
        if (currentFinancialOverview == null) {
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
            renderDailyRevenueBars(List.of());
            updatePaymentMethodsLegend(new HashMap<>(), 0L);
            return;
        }

        BigDecimal periodIncome = AdminFormatUtils.defaultAmount(currentFinancialOverview.getPeriodIncome());
        BigDecimal commission = periodIncome.multiply(new BigDecimal("0.20"));
        long processed = currentFinancialOverview.getProcessedPayments();
        long total = Math.max(currentFinancialOverview.getTotalPayments(), 0);
        long refunded = currentFinancialOverview.getRefundedPayments();
        long failed = currentFinancialOverview.getFailedPayments();

        BigDecimal avgTicket = processed <= 0 ? BigDecimal.ZERO : periodIncome.divide(BigDecimal.valueOf(processed), 2, RoundingMode.HALF_UP);
        long paidDrivers = allPayments.stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.PROCESSED)
                .map(payment -> AdminFormatUtils.normalize(payment.getDriverName()))
                .filter(value -> !value.isBlank())
                .distinct()
                .count();
        double processedRate = total == 0 ? 0 : (processed * 100.0) / total;
        double failedRate = total == 0 ? 0 : (failed * 100.0) / total;

        revenuePeriodLabel.setText("Receita Total - " + AdminFormatUtils.prettyFinancialPeriod(currentFinancialPeriod));
        revenueTotalLabel.setText(AdminFormatUtils.formatCurrency(periodIncome));
        revenueTrendLabel.setText("Taxa de sucesso: " + AdminFormatUtils.formatPercent(processedRate) + " | Pagamentos: " + total);
        netRevenueValueLabel.setText(AdminFormatUtils.formatCurrency(periodIncome));
        platformCommissionValueLabel.setText(AdminFormatUtils.formatCurrency(commission));
        billedTripsValueLabel.setText(String.valueOf(processed));
        avgTicketValueLabel.setText(AdminFormatUtils.formatCurrency(avgTicket));
        processedPaymentsValueLabel.setText(String.valueOf(processed));
        refundedPaymentsValueLabel.setText(String.valueOf(refunded));
        failedRateValueLabel.setText(AdminFormatUtils.formatPercent(failedRate));
        paidDriversValueLabel.setText(String.valueOf(paidDrivers));

        renderDailyRevenueBars(allPayments);
        updatePaymentMethodsLegend(groupPaymentMethods(allPayments), total);
    }

    private void updatePaymentDetailsPanel(AdminPaymentByTripDTO selectedPayment) {
        if (selectedPayment == null) {
            clearDetails();
            return;
        }

        setDetailsVisible(true);

        detailTitleLabel.setText("Detalhe do pagamento");
        detailInitialsLabel.setText(selectedPayment.getPaymentId() == null ? "--" : "#" + selectedPayment.getPaymentId());
        detailNameLabel.setText(AdminFormatUtils.formatPaymentAmount(selectedPayment));
        detailEmailLabel.setText("Cliente: " + AdminFormatUtils.fallback(selectedPayment.getClientName()));
        detailStatusLabel.setText(AdminFormatUtils.prettyPaymentStatus(selectedPayment.getStatus()));
        detailRoleLabel.setText(AdminFormatUtils.prettyPaymentMethod(selectedPayment.getPaymentMethodType()));
        detailPhoneValueLabel.setText("Metodo: " + AdminFormatUtils.prettyPaymentMethod(selectedPayment.getPaymentMethodType()));
        detailCreatedValueLabel.setText(selectedPayment.getPaymentDate() == null ? "-" : PAYMENT_DATE_FORMAT.format(selectedPayment.getPaymentDate()));
        detailCardOneTitleLabel.setText("ID Viagem");
        detailCardOneValueLabel.setText(selectedPayment.getTripId() == null ? "-" : "#" + selectedPayment.getTripId());
        detailCardTwoTitleLabel.setText("Valor");
        detailCardTwoValueLabel.setText(AdminFormatUtils.formatPaymentAmount(selectedPayment));
        detailCardThreeTitleLabel.setText("Estado");
        detailCardThreeValueLabel.setText(AdminFormatUtils.prettyPaymentStatus(selectedPayment.getStatus()));
        detailCardFourTitleLabel.setText("Moeda");
        detailCardFourValueLabel.setText(AdminFormatUtils.fallback(selectedPayment.getCurrencyCode()));
        detailReferenceTitleLabel.setText("Taxa aplicada");
        detailReferenceValueLabel.setText(selectedPayment.getTaxRateApplied() == null ? "-" : selectedPayment.getTaxRateApplied().multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP) + "%");
        detailExtraOneTitleLabel.setText("Motorista");
        detailExtraOneValueLabel.setText(AdminFormatUtils.fallback(selectedPayment.getDriverName()));
        detailExtraTwoTitleLabel.setText("Data pagamento");
        detailExtraTwoValueLabel.setText(selectedPayment.getPaymentDate() == null ? "-" : PAYMENT_DATE_FORMAT.format(selectedPayment.getPaymentDate()));
        detailExtraThreeTitleLabel.setText("Periodo");
        detailExtraThreeValueLabel.setText(AdminFormatUtils.prettyFinancialPeriod(currentFinancialPeriod));
    }

    private void renderDailyRevenueBars(List<AdminPaymentByTripDTO> payments) {
        dailyBarsContainer.getChildren().clear();
        List<RevenueBucket> buckets = buildRevenueBuckets(payments);
        BigDecimal maxValue = buckets.stream().map(RevenueBucket::value).reduce(BigDecimal.ZERO, BigDecimal::max);
        if (maxValue.compareTo(BigDecimal.ZERO) == 0) {
            dailyBarsContainer.setAlignment(Pos.CENTER_LEFT);
            Label emptyStateLabel = new Label("Sem receita processada no periodo selecionado.");
            emptyStateLabel.getStyleClass().add("financial-empty-state");
            dailyBarsContainer.getChildren().add(emptyStateLabel);
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
            Tooltip.install(bar, new Tooltip(bucket.tooltip() + " | " + AdminFormatUtils.formatCurrency(bucket.value())));
            column.getChildren().addAll(valueLabel, bar, bucketLabel);
            dailyBarsContainer.getChildren().add(column);
        }
    }

    private List<RevenueBucket> buildRevenueBuckets(List<AdminPaymentByTripDTO> payments) {
        List<AdminPaymentByTripDTO> processedPayments = payments == null ? List.of() : payments.stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.PROCESSED)
                .filter(payment -> payment.getPaymentDate() != null)
                .sorted(Comparator.comparing(AdminPaymentByTripDTO::getPaymentDate))
                .toList();
        return switch (revenueBucketType) {
            case DAY -> buildDayBuckets(processedPayments);
            case WEEK -> buildWeekBuckets(processedPayments);
            case MONTH -> buildMonthBuckets(processedPayments);
            case YEAR -> buildYearBuckets(processedPayments);
            case ALL -> buildAllBuckets(processedPayments);
        };
    }

    private List<RevenueBucket> buildDayBuckets(List<AdminPaymentByTripDTO> payments) {
        List<RevenueBucket> buckets = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();
        int bucketHours = 3;
        int bucketCount = 8;
        for (int i = bucketCount - 1; i >= 0; i--) {
            LocalDateTime start = now.minusHours((long) (i + 1) * bucketHours);
            LocalDateTime end = now.minusHours((long) i * bucketHours);
            BigDecimal value = sumPaymentsBetween(payments, start, end);
            buckets.add(new RevenueBucket(String.format(Locale.ROOT, "%02dh", start.getHour()), start.format(PAYMENT_DATE_FORMAT) + " - " + end.format(PAYMENT_DATE_FORMAT), value));
        }
        return buckets;
    }

    private List<RevenueBucket> buildWeekBuckets(List<AdminPaymentByTripDTO> payments) {
        List<RevenueBucket> buckets = new ArrayList<>();
        LocalDate today = LocalDate.now();
        for (int i = 6; i >= 0; i--) {
            LocalDate day = today.minusDays(i);
            BigDecimal value = sumPaymentsForDay(payments, day);
            String label = day.format(DAY_LABEL_FORMAT);
            buckets.add(new RevenueBucket(label, "Dia " + label, value));
        }
        return buckets;
    }

    private List<RevenueBucket> buildMonthBuckets(List<AdminPaymentByTripDTO> payments) {
        List<RevenueBucket> buckets = new ArrayList<>();
        LocalDate end = LocalDate.now();
        LocalDate start = end.minusDays(29);
        for (int i = 0; i < 30; i++) {
            LocalDate day = start.plusDays(i);
            BigDecimal value = sumPaymentsForDay(payments, day);
            buckets.add(new RevenueBucket(String.valueOf(day.getDayOfMonth()), "Dia " + day.format(DAY_LABEL_FORMAT), value));
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
            BigDecimal value = sumPaymentsBetween(payments, start, end);
            String label = month.format(MONTH_LABEL_FORMAT);
            String tooltip = month.format(DateTimeFormatter.ofPattern("MMMM yyyy", Locale.forLanguageTag("pt-PT")));
            buckets.add(new RevenueBucket(label, tooltip, value));
        }
        return buckets;
    }

    private List<RevenueBucket> buildAllBuckets(List<AdminPaymentByTripDTO> payments) {
        List<RevenueBucket> buckets = new ArrayList<>();
        int currentYear = LocalDate.now().getYear();
        int minYear = payments.stream().mapToInt(payment -> payment.getPaymentDate().getYear()).min().orElse(currentYear - 4);
        int startYear = Math.min(minYear, currentYear - 4);
        for (int year = startYear; year <= currentYear; year++) {
            LocalDateTime start = LocalDate.of(year, 1, 1).atStartOfDay();
            LocalDateTime end = LocalDate.of(year + 1, 1, 1).atStartOfDay();
            BigDecimal value = sumPaymentsBetween(payments, start, end);
            buckets.add(new RevenueBucket(String.valueOf(year), "Ano " + year, value));
        }
        return buckets;
    }

    private BigDecimal sumPaymentsForDay(List<AdminPaymentByTripDTO> payments, LocalDate day) {
        return payments.stream()
                .filter(payment -> payment.getPaymentDate().toLocalDate().equals(day))
                .map(payment -> AdminFormatUtils.defaultAmount(payment.getAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumPaymentsBetween(List<AdminPaymentByTripDTO> payments, LocalDateTime start, LocalDateTime end) {
        return payments.stream()
                .filter(payment -> !payment.getPaymentDate().isBefore(start))
                .filter(payment -> payment.getPaymentDate().isBefore(end))
                .map(payment -> AdminFormatUtils.defaultAmount(payment.getAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private void updatePaymentMethodsLegend(Map<String, Long> groupedMethods, long totalPayments) {
        List<Map.Entry<String, Long>> sorted = groupedMethods.entrySet().stream().sorted((left, right) -> Long.compare(right.getValue(), left.getValue())).limit(4).toList();
        setPaymentMethodLegendRow(sorted, 0, methodOneLabel, methodOnePercentLabel, totalPayments);
        setPaymentMethodLegendRow(sorted, 1, methodTwoLabel, methodTwoPercentLabel, totalPayments);
        setPaymentMethodLegendRow(sorted, 2, methodThreeLabel, methodThreePercentLabel, totalPayments);
        setPaymentMethodLegendRow(sorted, 3, methodFourLabel, methodFourPercentLabel, totalPayments);
    }

    private void setPaymentMethodLegendRow(List<Map.Entry<String, Long>> sorted, int index, Label methodLabel, Label percentLabel, long totalPayments) {
        if (index >= sorted.size()) {
            methodLabel.setText("-");
            percentLabel.setText("0%");
            return;
        }
        Map.Entry<String, Long> entry = sorted.get(index);
        methodLabel.setText(AdminFormatUtils.prettyPaymentMethod(entry.getKey()));
        double percentage = totalPayments == 0 ? 0 : (entry.getValue() * 100.0) / totalPayments;
        percentLabel.setText(AdminFormatUtils.formatPercent(percentage));
    }

    private Map<String, Long> groupPaymentMethods(List<AdminPaymentByTripDTO> payments) {
        Map<String, Long> grouped = new HashMap<>();
        for (AdminPaymentByTripDTO payment : payments) {
            String key = AdminFormatUtils.normalize(AdminFormatUtils.prettyPaymentMethod(payment.getPaymentMethodType()));
            grouped.put(key, grouped.getOrDefault(key, 0L) + 1);
        }
        return grouped;
    }

    private void setFinancialPeriod(AdminFinancialPeriod period) {
        if (period == null || period == currentFinancialPeriod) {
            return;
        }
        currentFinancialPeriod = period;
        if (periodComboBox != null) {
            periodComboBox.setValue(period);
        }
        revenueBucketType = toBucketType(period);
        refreshSectionData();
    }

    private RevenueBucketType toBucketType(AdminFinancialPeriod period) {
        return switch (period) {
            case DAY -> RevenueBucketType.DAY;
            case WEEK -> RevenueBucketType.WEEK;
            case MONTH -> RevenueBucketType.MONTH;
            case YEAR -> RevenueBucketType.YEAR;
            case ALL -> RevenueBucketType.ALL;
        };
    }

    private void setPaymentStatusFilter(PaymentStatus status) {
        currentPaymentStatusFilter = status;
        applyFilters();
    }

    private void updateFilterLabels() {
        long pendingCount = allPayments.stream().filter(payment -> payment.getStatus() == PaymentStatus.PENDING).count();
        long processedCount = allPayments.stream().filter(payment -> payment.getStatus() == PaymentStatus.PROCESSED).count();
        long failedCount = allPayments.stream().filter(payment -> payment.getStatus() == PaymentStatus.FAILED).count();
        long refundedCount = allPayments.stream().filter(payment -> payment.getStatus() == PaymentStatus.REFUNDED).count();

        if (listInfoLabel != null) {
            listInfoLabel.setText("A mostrar " + filteredPayments.size() + " de " + allPayments.size() + " pagamentos");
        }
        periodAllButton.setText("Todos (" + allPayments.size() + ")");
        periodDayButton.setText("Hoje (" + processedCount + ")");
        periodWeekButton.setText("7 dias (" + pendingCount + ")");
        periodMonthButton.setText("Este mes (" + failedCount + ")");
        periodYearButton.setText("Este ano (" + refundedCount + ")");
        paymentsTableTitle.setText("Ultimos Pagamentos");
    }

    private void populateTaxRateFields(AdminTaxRateDTO taxRate) {
        if (taxRate == null) {
            modalTaxRateNameField.clear();
            modalTaxRateValueField.clear();
            modalTaxRateDescriptionField.clear();
            modalTaxRateActiveCheck.setSelected(false);
            return;
        }
        modalTaxRateNameField.setText(AdminFormatUtils.fallback(taxRate.getName()).equals("-") ? "" : taxRate.getName());
        modalTaxRateValueField.setText(taxRate.getRate() == null ? "" : taxRate.getRate().toPlainString());
        modalTaxRateDescriptionField.setText(AdminFormatUtils.fallback(taxRate.getDescription()).equals("-") ? "" : taxRate.getDescription());
        modalTaxRateActiveCheck.setSelected(Boolean.TRUE.equals(taxRate.getActive()));
    }

    private void persistTaxRateUpdate() {
        if (adminService == null) {
            return;
        }
        try {
            AdminTaxRateDTO selectedTaxRate = modalTaxRateCombo.getValue();
            if (selectedTaxRate == null || selectedTaxRate.getId() == null) {
                showModalError("Selecione uma taxa de IVA valida.");
                return;
            }

            BigDecimal rate = parseRequiredDecimal(modalTaxRateValueField.getText(), "Taxa de IVA");
            adminService.updateTaxRate(selectedTaxRate.getId(), new AdminTaxRateCommand(
                    modalTaxRateNameField.getText(),
                    rate,
                    modalTaxRateDescriptionField.getText(),
                    modalTaxRateActiveCheck.isSelected()));
            hideModal();
            refreshSectionData();
            showFeedback("Taxa de IVA atualizada com sucesso.", false);
        } catch (Exception exception) {
            showModalError("Falha ao atualizar taxa de IVA: " + exception.getMessage());
        }
    }

    private void showFeedback(String message, boolean isError) {
        feedbackLabel.setText(message);
        feedbackLabel.getStyleClass().removeAll("feedback-success", "feedback-error");
        feedbackLabel.getStyleClass().add(isError ? "feedback-error" : "feedback-success");
    }

    private void showModal() {
        modalController.show();
    }

    private void hideModal() {
        modalController.hide();
    }

    private void clearModalError() {
        modalController.clearError();
    }

    private void showModalError(String message) {
        modalController.showError(message);
    }

    private void clearDetails() {
        detailInitialsLabel.setText("EUR");
        detailTitleLabel.setText("Resumo financeiro");
        detailNameLabel.setText("Rendimentos e pagamentos");
        detailEmailLabel.setText("-");
        detailStatusLabel.setText("-");
        detailRoleLabel.setText("Financeiro");
        detailPhoneValueLabel.setText("-");
        detailCreatedValueLabel.setText("-");
        detailCardOneTitleLabel.setText("Rend. total");
        detailCardOneValueLabel.setText("-");
        detailCardTwoTitleLabel.setText("Rend. periodo");
        detailCardTwoValueLabel.setText("-");
        detailCardThreeTitleLabel.setText("Processados");
        detailCardThreeValueLabel.setText("-");
        detailCardFourTitleLabel.setText("Pendentes");
        detailCardFourValueLabel.setText("-");
        detailReferenceTitleLabel.setText("Falhados");
        detailReferenceValueLabel.setText("-");
        detailExtraOneTitleLabel.setText("Reembolsados");
        detailExtraOneValueLabel.setText("-");
        detailExtraTwoTitleLabel.setText("Top IVA");
        detailExtraTwoValueLabel.setText("-");
        detailExtraThreeTitleLabel.setText("Acao");
        detailExtraThreeValueLabel.setText("-");
    }

    private void setDetailsVisible(boolean visible) {
        if (detailPanel == null) {
            return;
        }
        detailPanel.setVisible(visible);
        detailPanel.setManaged(visible);
    }

    private void updateFinancialDetailPanel() {
        if (currentFinancialOverview == null) {
            clearDetails();
            return;
        }

        detailInitialsLabel.setText("EUR");
        detailTitleLabel.setText("Resumo financeiro");
        detailNameLabel.setText("Periodo: " + AdminFormatUtils.prettyFinancialPeriod(currentFinancialPeriod));
        detailEmailLabel.setText("Total pagamentos: " + currentFinancialOverview.getTotalPayments());
        detailStatusLabel.setText("Processados: " + currentFinancialOverview.getProcessedPayments());
        detailRoleLabel.setText("Financeiro");
        detailPhoneValueLabel.setText("Pendentes: " + currentFinancialOverview.getPendingPayments());
        detailCreatedValueLabel.setText(LocalDateTime.now().format(PAYMENT_DATE_FORMAT));

        detailCardOneTitleLabel.setText("Rend. total");
        detailCardOneValueLabel.setText(AdminFormatUtils.formatCurrency(AdminFormatUtils.defaultAmount(currentFinancialOverview.getTotalIncome())));
        detailCardTwoTitleLabel.setText("Rend. periodo");
        detailCardTwoValueLabel.setText(AdminFormatUtils.formatCurrency(AdminFormatUtils.defaultAmount(currentFinancialOverview.getPeriodIncome())));
        detailCardThreeTitleLabel.setText("Processados");
        detailCardThreeValueLabel.setText(String.valueOf(currentFinancialOverview.getProcessedPayments()));
        detailCardFourTitleLabel.setText("Pendentes");
        detailCardFourValueLabel.setText(String.valueOf(currentFinancialOverview.getPendingPayments()));
        detailReferenceTitleLabel.setText("Falhados");
        detailReferenceValueLabel.setText(String.valueOf(currentFinancialOverview.getFailedPayments()));
        detailExtraOneTitleLabel.setText("Reembolsados");
        detailExtraOneValueLabel.setText(String.valueOf(currentFinancialOverview.getRefundedPayments()));

        AdminTaxRateDTO topTaxRate = allTaxRates.stream()
                .max(Comparator.comparing(AdminTaxRateDTO::getRate, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);
        detailExtraTwoTitleLabel.setText("Top IVA");
        detailExtraTwoValueLabel.setText(topTaxRate == null ? "-" : AdminFormatUtils.formatTaxRateDisplay(topTaxRate));
        detailExtraThreeTitleLabel.setText("Acao");
        detailExtraThreeValueLabel.setText("Use exportar para gerar relatorio.");
    }

    private String formatCompactCurrency(BigDecimal value) {
        BigDecimal safeValue = AdminFormatUtils.defaultAmount(value);
        if (safeValue.compareTo(BigDecimal.ZERO) == 0) {
            return "EUR 0";
        }
        if (safeValue.compareTo(new BigDecimal("1000")) >= 0) {
            BigDecimal compact = safeValue.divide(new BigDecimal("1000"), 1, RoundingMode.HALF_UP);
            return "EUR " + compact + "k";
        }
        return "EUR " + safeValue.setScale(0, RoundingMode.HALF_UP);
    }

    private FinancialExportService.FinancialExportSnapshot buildFinancialExportSnapshot() {
        Map<String, Long> methods = groupPaymentMethods(allPayments);
        String scopeDescription = AdminFormatUtils.prettyFinancialPeriod(currentFinancialPeriod) + " | " + filteredPayments.size() + " de " + allPayments.size() + " pagamentos";
        return new FinancialExportService.FinancialExportSnapshot(LocalDateTime.now(), scopeDescription, currentFinancialOverview, List.copyOf(allPayments), List.copyOf(allTaxRates), methods);
    }

    private Path resolveExportDirectory() throws IOException {
        Path documentsDir = Path.of(System.getProperty("user.home"), "Documents", "obar-exports");
        Files.createDirectories(documentsDir);
        return documentsDir;
    }

    private BigDecimal parseRequiredDecimal(String rawValue, String fieldName) {
        String safe = rawValue == null ? "" : rawValue.trim();
        if (safe.isBlank()) {
            throw new IllegalArgumentException(fieldName + " e obrigatorio.");
        }
        try {
            return new BigDecimal(safe);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Valor monetario invalido: " + safe);
        }
    }

    private void openTaxRateEditModal() {
        if (allTaxRates.isEmpty()) {
            showFeedback("Nao existem taxas de IVA para editar.", true);
            return;
        }

        modalController.prepareForForm("Editar taxa de IVA");
        modalTaxRateFormSection.setVisible(true);
        modalTaxRateFormSection.setManaged(true);
        modalTaxRateCombo.setItems(FXCollections.observableArrayList(allTaxRates));
        modalTaxRateCombo.getSelectionModel().selectFirst();
        populateTaxRateFields(modalTaxRateCombo.getValue());
    }
}
