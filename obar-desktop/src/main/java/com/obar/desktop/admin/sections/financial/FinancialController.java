package com.obar.desktop.admin.sections.financial;

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
import com.obar.desktop.admin.shared.AdminParseUtils;
import com.obar.desktop.admin.shared.DetailPanelBinder;
import com.obar.desktop.admin.shared.DetailPanelBinder.DetailViewModel;
import com.obar.desktop.admin.shared.FilterChipManager;
import com.obar.desktop.admin.sections.financial.RevenueBucketBuilder.RevenueBucket;
import com.obar.model.enums.PaymentStatus;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.event.ActionEvent;
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
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class FinancialController implements AdminSectionController {

    private static final DateTimeFormatter PAYMENT_DATE_FORMAT =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.forLanguageTag("pt-PT"));
    private static final DateTimeFormatter EXPORT_TIMESTAMP_FORMAT =
            DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss", Locale.ROOT);

    private final ObservableList<AdminPaymentByTripDTO> allPayments = FXCollections.observableArrayList();
    private final FilteredList<AdminPaymentByTripDTO> filteredPayments = new FilteredList<>(allPayments, p -> true);
    private final ObservableList<AdminTaxRateDTO> allTaxRates = FXCollections.observableArrayList();
    private final FinancialExportService exportService = new FinancialExportService();
    private final RevenueBucketBuilder bucketBuilder = new RevenueBucketBuilder();

    private AdminService adminService;
    private AdminFinancialPeriod currentFinancialPeriod = AdminFinancialPeriod.MONTH;
    private AdminFinancialOverviewDTO currentFinancialOverview;
    private PaymentStatus currentPaymentStatusFilter;
    private AdminModalController modalController;
    private DetailPanelBinder detailBinder;
    private FilterChipManager<PaymentStatus> statusChips;

    // — table & toolbar —
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
    @FXML private Label listInfoLabel;
    @FXML private Label feedbackLabel;
    @FXML private Button exportPdfButton;
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

    // — detail panel labels (wired into DetailPanelBinder) —
    @FXML private VBox detailPanel;
    @FXML private Label detailInitialsLabel;
    @FXML private Label detailTitleLabel;
    @FXML private Label detailNameLabel;
    @FXML private Label detailEmailLabel;
    @FXML private Label detailStatusLabel;
    @FXML private Label detailRoleLabel;
    @FXML private Label detailPhoneValueLabel;
    @FXML private Label detailCreatedValueLabel;
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
    @FXML private Label detailExtraOneTitleLabel;
    @FXML private Label detailExtraOneValueLabel;
    @FXML private Label detailExtraTwoTitleLabel;
    @FXML private Label detailExtraTwoValueLabel;
    @FXML private Label detailExtraThreeTitleLabel;
    @FXML private Label detailExtraThreeValueLabel;

    // — modal fields (bound from shared modal) —
    @FXML private AdminModalIncludeController sharedModalController;
    @FXML private VBox modalTaxRateFormSection;
    @FXML private Label modalDeleteMessageLabel;
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

        statusChips = new FilterChipManager<PaymentStatus>()
                .add(periodAllButton,   null)
                .add(periodDayButton,   PaymentStatus.PROCESSED)
                .add(periodWeekButton,  PaymentStatus.PENDING)
                .add(periodMonthButton, PaymentStatus.FAILED)
                .add(periodYearButton,  PaymentStatus.REFUNDED);

        paymentsTable.setItems(filteredPayments);
        setupTaxRateCombo();
        searchField.textProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        paymentsTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, previous, current) -> onPaymentSelectionChanged(current));
        setupColumns();
        refreshSectionData();
    }

    private void bindModalFields() {
        if (sharedModalController == null) {
            throw new IllegalStateException("Shared modal controller was not injected.");
        }
        modalTaxRateFormSection      = sharedModalController.getModalTaxRateFormSection();
        modalDeleteMessageLabel      = sharedModalController.getModalDeleteMessageLabel();
        modalTaxRateCombo            = sharedModalController.getModalTaxRateCombo();
        modalTaxRateNameField        = sharedModalController.getModalTaxRateNameField();
        modalTaxRateValueField       = sharedModalController.getModalTaxRateValueField();
        modalTaxRateDescriptionField = sharedModalController.getModalTaxRateDescriptionField();
        modalTaxRateActiveCheck      = sharedModalController.getModalTaxRateActiveCheck();
    }

    private void setupTaxRateCombo() {
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
    }

    @Override
    public void onSectionActivated() {
        refreshSectionData();
    }

    // — period handlers —

    @FXML public void handlePeriodChanged()        { setFinancialPeriod(currentFinancialPeriod); }
    @FXML public void handleFinancialPeriodDay()   { setFinancialPeriod(AdminFinancialPeriod.DAY); }
    @FXML public void handleFinancialPeriodWeek()  { setFinancialPeriod(AdminFinancialPeriod.WEEK); }
    @FXML public void handleFinancialPeriodMonth() { setFinancialPeriod(AdminFinancialPeriod.MONTH); }
    @FXML public void handleFinancialPeriodYear()  { setFinancialPeriod(AdminFinancialPeriod.YEAR); }
    @FXML public void handleFinancialPeriodAll()   { setFinancialPeriod(AdminFinancialPeriod.ALL); }

    // — filter handlers —

    @FXML public void handleFilterAll()      { setPaymentStatusFilter(null); }
    @FXML public void handleFilterActive()   { setPaymentStatusFilter(PaymentStatus.PROCESSED); }
    @FXML public void handleFilterInactive() { setPaymentStatusFilter(PaymentStatus.FAILED); }
    @FXML public void handleFilterBlocked()  { setPaymentStatusFilter(PaymentStatus.REFUNDED); }
    @FXML public void handleFilterPending()  { setPaymentStatusFilter(PaymentStatus.PENDING); }
    @FXML public void handleEditTaxRate()    { openTaxRateEditModal(); }

    @FXML
    public void handleExportSection(ActionEvent event) {
        try {
            if (adminService == null) { return; }
            FinancialExportService.FinancialExportSnapshot snapshot = buildExportSnapshot();
            Path exportDir = resolveExportDirectory();
            String timestamp = EXPORT_TIMESTAMP_FORMAT.format(LocalDateTime.now());
            boolean exportPdf = event != null && event.getSource() == exportPdfButton;
            Path exportPath = exportDir.resolve("relatorio_financeiro_" + timestamp + (exportPdf ? ".pdf" : ".csv"));
            if (exportPdf) { exportService.exportPdf(snapshot, exportPath); }
            else           { exportService.exportCsv(snapshot, exportPath); }
            showFeedback("Exportacao concluida: " + exportPath.toAbsolutePath(), false);
        } catch (Exception exception) {
            showFeedback("Falha na exportacao financeira: " + exception.getMessage(), true);
        }
    }

    // — modal handlers —

    @FXML public void handleModalCancel()        { modalController.hide(); }
    @FXML public void handleModalSave()          { persistTaxRateUpdate(); }
    @FXML public void handleModalConfirmDelete() { modalController.hide(); }

    @FXML public void handleCloseDetailPanel() {
        paymentsTable.getSelectionModel().clearSelection();
        detailBinder.clear(financialEmptyState());
    }

    // — private helpers —

    private void setFinancialPeriod(AdminFinancialPeriod period) {
        if (period == null || period == currentFinancialPeriod) { return; }
        currentFinancialPeriod = period;
        refreshSectionData();
    }

    private void setPaymentStatusFilter(PaymentStatus status) {
        currentPaymentStatusFilter = status;
        statusChips.setActive(status);
        applyFilters();
    }

    private void refreshSectionData() {
        if (adminService == null) { return; }
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

    private void applyFilters() {
        String query = AdminFormatUtils.normalize(searchField.getText());
        filteredPayments.setPredicate(p -> matchesPaymentStatus(p) && matchesPaymentQuery(p, query));
        updateFilterLabels();
    }

    private boolean matchesPaymentStatus(AdminPaymentByTripDTO payment) {
        return currentPaymentStatusFilter == null || payment.getStatus() == currentPaymentStatusFilter;
    }

    private boolean matchesPaymentQuery(AdminPaymentByTripDTO payment, String query) {
        if (query.isBlank()) { return true; }
        String paymentId = payment.getPaymentId() == null ? "" : String.valueOf(payment.getPaymentId());
        String tripId    = payment.getTripId() == null ? "" : String.valueOf(payment.getTripId());
        return AdminFormatUtils.normalize(paymentId).contains(query)
                || AdminFormatUtils.normalize(tripId).contains(query)
                || AdminFormatUtils.normalize(payment.getClientName()).contains(query)
                || AdminFormatUtils.normalize(payment.getDriverName()).contains(query)
                || AdminFormatUtils.normalize(AdminFormatUtils.prettyPaymentMethod(payment.getPaymentMethodType())).contains(query)
                || AdminFormatUtils.normalize(AdminFormatUtils.prettyPaymentStatus(payment.getStatus())).contains(query);
    }

    private void updateFilterLabels() {
        long pending   = allPayments.stream().filter(p -> p.getStatus() == PaymentStatus.PENDING).count();
        long processed = allPayments.stream().filter(p -> p.getStatus() == PaymentStatus.PROCESSED).count();
        long failed    = allPayments.stream().filter(p -> p.getStatus() == PaymentStatus.FAILED).count();
        long refunded  = allPayments.stream().filter(p -> p.getStatus() == PaymentStatus.REFUNDED).count();

        if (listInfoLabel != null) {
            listInfoLabel.setText("A mostrar " + filteredPayments.size() + " de " + allPayments.size() + " pagamentos");
        }
        periodAllButton.setText("Todos (" + allPayments.size() + ")");
        periodDayButton.setText("Hoje (" + processed + ")");
        periodWeekButton.setText("7 dias (" + pending + ")");
        periodMonthButton.setText("Este mes (" + failed + ")");
        periodYearButton.setText("Este ano (" + refunded + ")");
        paymentsTableTitle.setText("Ultimos Pagamentos");
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
            renderRevenueBars(List.of());
            updatePaymentMethodsLegend(new HashMap<>(), 0L);
            return;
        }

        BigDecimal periodIncome = AdminFormatUtils.defaultAmount(currentFinancialOverview.getPeriodIncome());
        BigDecimal commission   = periodIncome.multiply(new BigDecimal("0.20"));
        long processed = currentFinancialOverview.getProcessedPayments();
        long total     = Math.max(currentFinancialOverview.getTotalPayments(), 0);
        long refunded  = currentFinancialOverview.getRefundedPayments();
        long failed    = currentFinancialOverview.getFailedPayments();
        BigDecimal avgTicket = processed <= 0 ? BigDecimal.ZERO
                : periodIncome.divide(BigDecimal.valueOf(processed), 2, RoundingMode.HALF_UP);
        long paidDrivers = allPayments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.PROCESSED)
                .map(p -> AdminFormatUtils.normalize(p.getDriverName()))
                .filter(v -> !v.isBlank())
                .distinct().count();
        double processedRate = total == 0 ? 0 : (processed * 100.0) / total;
        double failedRate    = total == 0 ? 0 : (failed * 100.0) / total;

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

        renderRevenueBars(allPayments);
        updatePaymentMethodsLegend(groupPaymentMethods(allPayments), total);
    }

    private void renderRevenueBars(List<AdminPaymentByTripDTO> payments) {
        dailyBarsContainer.getChildren().clear();
        List<RevenueBucket> buckets = bucketBuilder.build(currentFinancialPeriod, payments);
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
            if (ratio >= 0.75) { bar.getStyleClass().add("financial-bar-strong"); }
            else if (ratio >= 0.45) { bar.getStyleClass().add("financial-bar-medium"); }
            bar.setPrefWidth(barWidth); bar.setMinWidth(barWidth); bar.setMaxWidth(barWidth);
            bar.setPrefHeight(6 + (ratio * 88));

            Label bucketLabel = new Label(bucket.label());
            bucketLabel.getStyleClass().add("financial-bar-label");
            Tooltip.install(bar, new Tooltip(bucket.tooltip() + " | " + AdminFormatUtils.formatCurrency(bucket.value())));

            column.getChildren().addAll(valueLabel, bar, bucketLabel);
            dailyBarsContainer.getChildren().add(column);
        }
    }

    private void onPaymentSelectionChanged(AdminPaymentByTripDTO payment) {
        if (payment == null) { detailBinder.clear(financialEmptyState()); return; }
        String taxRate = payment.getTaxRateApplied() == null ? "-"
                : payment.getTaxRateApplied().multiply(new BigDecimal("100")).setScale(2, RoundingMode.HALF_UP) + "%";
        detailBinder.bind(new DetailViewModel.Builder()
                .initials(payment.getPaymentId() == null ? "--" : "#" + payment.getPaymentId())
                .title("Detalhe do pagamento")
                .name(AdminFormatUtils.formatPaymentAmount(payment))
                .email("Cliente: " + AdminFormatUtils.fallback(payment.getClientName()))
                .status(AdminFormatUtils.prettyPaymentStatus(payment.getStatus()))
                .role(AdminFormatUtils.prettyPaymentMethod(payment.getPaymentMethodType()))
                .phone("Metodo: " + AdminFormatUtils.prettyPaymentMethod(payment.getPaymentMethodType()))
                .created(payment.getPaymentDate() == null ? "-" : PAYMENT_DATE_FORMAT.format(payment.getPaymentDate()))
                .card1("ID Viagem",  payment.getTripId() == null ? "-" : "#" + payment.getTripId())
                .card2("Valor",      AdminFormatUtils.formatPaymentAmount(payment))
                .card3("Estado",     AdminFormatUtils.prettyPaymentStatus(payment.getStatus()))
                .card4("Moeda",      AdminFormatUtils.fallback(payment.getCurrencyCode()))
                .ref("Taxa aplicada", taxRate)
                .extra1("Motorista",        AdminFormatUtils.fallback(payment.getDriverName()))
                .extra2("Data pagamento",   payment.getPaymentDate() == null ? "-" : PAYMENT_DATE_FORMAT.format(payment.getPaymentDate()))
                .extra3("Periodo",          AdminFormatUtils.prettyFinancialPeriod(currentFinancialPeriod))
                .build());
    }

    private void updateFinancialDetailPanel() {
        if (currentFinancialOverview == null) { detailBinder.clear(financialEmptyState()); return; }
        AdminTaxRateDTO topTax = allTaxRates.stream()
                .max(Comparator.comparing(AdminTaxRateDTO::getRate, Comparator.nullsLast(Comparator.naturalOrder())))
                .orElse(null);
        detailBinder.bind(new DetailViewModel.Builder()
                .initials("EUR").title("Resumo financeiro")
                .name("Periodo: " + AdminFormatUtils.prettyFinancialPeriod(currentFinancialPeriod))
                .email("Total pagamentos: " + currentFinancialOverview.getTotalPayments())
                .status("Processados: " + currentFinancialOverview.getProcessedPayments())
                .role("Financeiro")
                .phone("Pendentes: " + currentFinancialOverview.getPendingPayments())
                .created(LocalDateTime.now().format(PAYMENT_DATE_FORMAT))
                .card1("Rend. total",    AdminFormatUtils.formatCurrency(AdminFormatUtils.defaultAmount(currentFinancialOverview.getTotalIncome())))
                .card2("Rend. periodo",  AdminFormatUtils.formatCurrency(AdminFormatUtils.defaultAmount(currentFinancialOverview.getPeriodIncome())))
                .card3("Processados",    String.valueOf(currentFinancialOverview.getProcessedPayments()))
                .card4("Pendentes",      String.valueOf(currentFinancialOverview.getPendingPayments()))
                .ref("Falhados",         String.valueOf(currentFinancialOverview.getFailedPayments()))
                .extra1("Reembolsados",  String.valueOf(currentFinancialOverview.getRefundedPayments()))
                .extra2("Top IVA",       topTax == null ? "-" : AdminFormatUtils.formatTaxRateDisplay(topTax))
                .extra3("Acao",          "Use exportar para gerar relatorio.")
                .build());
    }

    private static DetailViewModel financialEmptyState() {
        return new DetailViewModel.Builder()
                .initials("EUR").title("Resumo financeiro").name("Sem dados financeiros")
                .role("Financeiro")
                .card1("Rend. total", "-").card2("Rend. periodo", "-")
                .card3("Processados", "-").card4("Pendentes", "-")
                .ref("Falhados", "-")
                .extra1("Reembolsados", "-").extra2("Top IVA", "-").extra3("Acao", "-")
                .build();
    }

    private void updatePaymentMethodsLegend(Map<String, Long> groupedMethods, long totalPayments) {
        List<Map.Entry<String, Long>> sorted = groupedMethods.entrySet().stream()
                .sorted((l, r) -> Long.compare(r.getValue(), l.getValue()))
                .limit(4).toList();
        setLegendRow(sorted, 0, methodOneLabel,   methodOnePercentLabel,   totalPayments);
        setLegendRow(sorted, 1, methodTwoLabel,   methodTwoPercentLabel,   totalPayments);
        setLegendRow(sorted, 2, methodThreeLabel, methodThreePercentLabel, totalPayments);
        setLegendRow(sorted, 3, methodFourLabel,  methodFourPercentLabel,  totalPayments);
    }

    private void setLegendRow(List<Map.Entry<String, Long>> sorted, int index,
                              Label methodLabel, Label percentLabel, long total) {
        if (index >= sorted.size()) { methodLabel.setText("-"); percentLabel.setText("0%"); return; }
        Map.Entry<String, Long> entry = sorted.get(index);
        methodLabel.setText(AdminFormatUtils.prettyPaymentMethod(entry.getKey()));
        percentLabel.setText(AdminFormatUtils.formatPercent(total == 0 ? 0 : (entry.getValue() * 100.0) / total));
    }

    private Map<String, Long> groupPaymentMethods(List<AdminPaymentByTripDTO> payments) {
        Map<String, Long> grouped = new HashMap<>();
        for (AdminPaymentByTripDTO p : payments) {
            String key = AdminFormatUtils.normalize(AdminFormatUtils.prettyPaymentMethod(p.getPaymentMethodType()));
            grouped.put(key, grouped.getOrDefault(key, 0L) + 1);
        }
        return grouped;
    }

    private void setupColumns() {
        paymentIdColumn.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getPaymentId() == null ? "-" : "#" + cd.getValue().getPaymentId()));
        paymentTripIdColumn.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getTripId() == null ? "-" : "#" + cd.getValue().getTripId()));
        paymentClientColumn.setCellValueFactory(cd -> new SimpleStringProperty(AdminFormatUtils.fallback(cd.getValue().getClientName())));
        paymentMethodColumn.setCellValueFactory(cd -> new SimpleStringProperty(AdminFormatUtils.prettyPaymentMethod(cd.getValue().getPaymentMethodType())));
        paymentAmountColumn.setCellValueFactory(cd -> new SimpleStringProperty(AdminFormatUtils.formatPaymentAmount(cd.getValue())));
        paymentDateColumn.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getPaymentDate() == null ? "-" : PAYMENT_DATE_FORMAT.format(cd.getValue().getPaymentDate())));
        paymentStatusColumn.setCellValueFactory(cd -> new SimpleStringProperty(AdminFormatUtils.prettyPaymentStatus(cd.getValue().getStatus())));
        paymentStatusColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("payment-processed", "payment-pending", "payment-failed", "payment-refunded");
                if (empty || item == null) { setText(null); return; }
                setText(item);
                if (getIndex() < 0 || getIndex() >= getTableView().getItems().size()) { return; }
                AdminPaymentByTripDTO row = getTableView().getItems().get(getIndex());
                if (row.getStatus() == null) { return; }
                switch (row.getStatus()) {
                    case PROCESSED -> getStyleClass().add("payment-processed");
                    case PENDING   -> getStyleClass().add("payment-pending");
                    case FAILED    -> getStyleClass().add("payment-failed");
                    case REFUNDED  -> getStyleClass().add("payment-refunded");
                }
            }
        });
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
        if (adminService == null) { return; }
        try {
            AdminTaxRateDTO selected = modalTaxRateCombo.getValue();
            if (selected == null || selected.getId() == null) { modalController.showError("Selecione uma taxa de IVA valida."); return; }
            BigDecimal rate = AdminParseUtils.parseRequiredDecimal(modalTaxRateValueField.getText(), "Taxa de IVA");
            adminService.updateTaxRate(selected.getId(), new AdminTaxRateCommand(
                    modalTaxRateNameField.getText(), rate,
                    modalTaxRateDescriptionField.getText(), modalTaxRateActiveCheck.isSelected()));
            modalController.hide();
            refreshSectionData();
            showFeedback("Taxa de IVA atualizada com sucesso.", false);
        } catch (Exception exception) {
            modalController.showError("Falha ao atualizar taxa de IVA: " + exception.getMessage());
        }
    }

    private void openTaxRateEditModal() {
        if (allTaxRates.isEmpty()) { showFeedback("Nao existem taxas de IVA para editar.", true); return; }
        modalController.prepareForForm("Editar taxa de IVA");
        modalTaxRateFormSection.setVisible(true);
        modalTaxRateFormSection.setManaged(true);
        modalTaxRateCombo.setItems(FXCollections.observableArrayList(allTaxRates));
        modalTaxRateCombo.getSelectionModel().selectFirst();
        populateTaxRateFields(modalTaxRateCombo.getValue());
    }

    private FinancialExportService.FinancialExportSnapshot buildExportSnapshot() {
        Map<String, Long> methods = groupPaymentMethods(allPayments);
        String scope = AdminFormatUtils.prettyFinancialPeriod(currentFinancialPeriod)
                + " | " + filteredPayments.size() + " de " + allPayments.size() + " pagamentos";
        return new FinancialExportService.FinancialExportSnapshot(
                LocalDateTime.now(), scope, currentFinancialOverview,
                List.copyOf(allPayments), List.copyOf(allTaxRates), methods);
    }

    private Path resolveExportDirectory() throws Exception {
        Path dir = Path.of(System.getProperty("user.home"), "Documents", "obar-exports");
        Files.createDirectories(dir);
        return dir;
    }

    private String formatCompactCurrency(BigDecimal value) {
        BigDecimal safe = AdminFormatUtils.defaultAmount(value);
        if (safe.compareTo(BigDecimal.ZERO) == 0) { return "EUR 0"; }
        if (safe.compareTo(new BigDecimal("1000")) >= 0) {
            return "EUR " + safe.divide(new BigDecimal("1000"), 1, RoundingMode.HALF_UP) + "k";
        }
        return "EUR " + safe.setScale(0, RoundingMode.HALF_UP);
    }

    private void showFeedback(String message, boolean isError) {
        feedbackLabel.setText(message);
        feedbackLabel.getStyleClass().removeAll("feedback-success", "feedback-error");
        feedbackLabel.getStyleClass().add(isError ? "feedback-error" : "feedback-success");
    }
}
