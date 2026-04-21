package com.obar.desktop.admin;

import com.obar.bll.admin.AdminService;
import com.obar.bll.admin.AdminFinancialOverviewDTO;
import com.obar.bll.admin.AdminFinancialPeriod;
import com.obar.bll.admin.AdminPaymentByTripDTO;
import com.obar.bll.admin.AdminPaymentStatusSummaryDTO;
import com.obar.bll.admin.AdminTaxRateCommand;
import com.obar.bll.admin.AdminTaxRateDTO;
import com.obar.bll.admin.AdminTripCommand;
import com.obar.bll.admin.AdminTripDTO;
import com.obar.bll.admin.AdminUserCommand;
import com.obar.bll.admin.AdminUserDTO;
import com.obar.bll.auth.AuthenticatedUserDto;
import com.lowagie.text.Document;
import com.lowagie.text.DocumentException;
import com.lowagie.text.Font;
import com.lowagie.text.FontFactory;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import com.obar.desktop.navigation.NavigationManager;
import com.obar.desktop.session.SessionManager;
import com.obar.model.enums.AccountStatus;
import com.obar.model.enums.PaymentStatus;
import com.obar.model.enums.TripStatus;
import com.obar.model.enums.TripType;
import com.obar.model.enums.UserType;
import javafx.event.ActionEvent;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Region;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.BufferedWriter;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * JavaFX controller for the admin desktop page.
 */
public class AdminController {

    private static final DateTimeFormatter TABLE_CREATED_AT_FORMAT = DateTimeFormatter.ofPattern("MMM yyyy",
            Locale.forLanguageTag("pt-PT"));
    private static final DateTimeFormatter DETAIL_CREATED_AT_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy HH:mm",
            Locale.forLanguageTag("pt-PT"));
        private static final DateTimeFormatter TRIP_REQUESTED_AT_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm",
            Locale.forLanguageTag("pt-PT"));
        private static final DateTimeFormatter PAYMENT_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm",
            Locale.forLanguageTag("pt-PT"));
        private static final DateTimeFormatter DAY_LABEL_FORMAT = DateTimeFormatter.ofPattern("dd/MM",
            Locale.forLanguageTag("pt-PT"));
        private static final DateTimeFormatter MONTH_LABEL_FORMAT = DateTimeFormatter.ofPattern("MMM",
            Locale.forLanguageTag("pt-PT"));
            private static final DateTimeFormatter EXPORT_TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss",
                Locale.ROOT);
            private static final DateTimeFormatter EXPORT_READABLE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm",
                Locale.forLanguageTag("pt-PT"));

        private record RevenueBucket(String label, String tooltip, BigDecimal value) {
        }

            private record FinancialExportSnapshot(
                LocalDateTime generatedAt,
                String scopeDescription,
                AdminFinancialOverviewDTO overview,
                List<AdminPaymentByTripDTO> payments,
                List<AdminTaxRateDTO> taxRates,
                Map<String, Long> paymentMethods) {
            }

    private enum ModalMode {
        NONE,
        CREATE,
        EDIT,
        CREATE_TRIP,
        EDIT_TRIP,
        EDIT_TAX_RATE,
        DELETE_TRIP_CONFIRM,
        DELETE_CONFIRM
    }

    private enum AdminSection {
        DRIVERS("Motoristas", "+ Novo Motorista", UserType.DRIVER, "Motorista"),
        CLIENTS("Clientes", "+ Novo Cliente", UserType.CLIENT, "Cliente"),
        TRIPS("Viagens", "+ Nova Viagem", null, "Viagem"),
        FINANCIAL("Relatorio Financeiro", "", null, "Financeiro");

        private final String title;
        private final String createLabel;
        private final UserType userType;
        private final String badgeLabel;

        AdminSection(String title, String createLabel, UserType userType, String badgeLabel) {
            this.title = title;
            this.createLabel = createLabel;
            this.userType = userType;
            this.badgeLabel = badgeLabel;
        }
    }

    private final AdminService adminService;
    private final ObservableList<AdminUserDTO> allUsers = FXCollections.observableArrayList();
    private final FilteredList<AdminUserDTO> filteredUsers = new FilteredList<>(allUsers, user -> true);
    private final ObservableList<AdminTripDTO> allTrips = FXCollections.observableArrayList();
    private final FilteredList<AdminTripDTO> filteredTrips = new FilteredList<>(allTrips, trip -> true);
        private final ObservableList<AdminPaymentByTripDTO> allPayments = FXCollections.observableArrayList();
        private final FilteredList<AdminPaymentByTripDTO> filteredPayments = new FilteredList<>(allPayments,
            payment -> true);
        private final ObservableList<AdminTaxRateDTO> allTaxRates = FXCollections.observableArrayList();

    private AdminSection currentSection = AdminSection.DRIVERS;
    private AccountStatus currentStatusFilter;
    private TripStatus currentTripStatusFilter;
        private PaymentStatus currentPaymentStatusFilter;
        private AdminFinancialPeriod currentFinancialPeriod = AdminFinancialPeriod.MONTH;
    private ModalMode modalMode = ModalMode.NONE;
    private AdminUserDTO modalTargetUser;
    private AdminTripDTO modalTargetTrip;
        private AdminFinancialOverviewDTO currentFinancialOverview;

    @FXML
    private Label currentSectionLabel;

    @FXML
    private Label sidebarUserInitialsLabel;

    @FXML
    private Label sidebarUserNameLabel;

    @FXML
    private Label sidebarUserRoleLabel;

    @FXML
    private Label feedbackLabel;

    @FXML
    private Label listInfoLabel;

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
    private Label detailPhoneValueLabel;

    @FXML
    private Label detailCreatedValueLabel;

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
    private VBox detailPanel;

    @FXML
    private TextField searchField;

    @FXML
    private TableView<AdminUserDTO> usersTable;

    @FXML
    private TableView<AdminTripDTO> tripsTable;

    @FXML
    private TableView<AdminPaymentByTripDTO> paymentsTable;

    @FXML
    private TableColumn<AdminUserDTO, String> nameColumn;

    @FXML
    private TableColumn<AdminUserDTO, String> userIdColumn;

    @FXML
    private TableColumn<AdminUserDTO, String> emailColumn;

    @FXML
    private TableColumn<AdminUserDTO, String> statusColumn;

    @FXML
    private TableColumn<AdminUserDTO, String> metricColumn;

    @FXML
    private TableColumn<AdminUserDTO, String> volumeColumn;

    @FXML
    private TableColumn<AdminUserDTO, String> referenceColumn;

    @FXML
    private TableColumn<AdminUserDTO, String> createdAtColumn;

    @FXML
    private TableColumn<AdminTripDTO, String> tripIdColumn;

    @FXML
    private TableColumn<AdminTripDTO, String> tripClientColumn;

    @FXML
    private TableColumn<AdminTripDTO, String> tripDriverColumn;

    @FXML
    private TableColumn<AdminTripDTO, String> tripStatusColumn;

    @FXML
    private TableColumn<AdminTripDTO, String> tripTypeColumn;

    @FXML
    private TableColumn<AdminTripDTO, String> tripPriceColumn;

    @FXML
    private TableColumn<AdminTripDTO, String> tripRequestedAtColumn;

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
    private VBox financialDashboard;

    @FXML
    private HBox searchFilterRow;

    @FXML
    private HBox tableActionRow;

    @FXML
    private Button exportPdfButton;

    @FXML
    private Button exportCsvButton;

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
    private Label paymentsTableTitle;

    @FXML
    private Button addUserButton;

    @FXML
    private Button editUserButton;

    @FXML
    private Button deleteUserButton;

    @FXML
    private Button motoristasSectionButton;

    @FXML
    private Button clientesSectionButton;

    @FXML
    private Button viagensSectionButton;

    @FXML
    private Button financeiraSectionButton;

    @FXML
    private ComboBox<AdminFinancialPeriod> periodComboBox;

    @FXML
    private Button filterAllButton;

    @FXML
    private Button filterActiveButton;

    @FXML
    private Button filterInactiveButton;

    @FXML
    private Button filterBlockedButton;

    @FXML
    private Button filterPendingButton;

    @FXML
    private StackPane modalOverlay;

    @FXML
    private Label modalTitleLabel;

    @FXML
    private Label modalErrorLabel;

    @FXML
    private VBox modalUserFormSection;

    @FXML
    private VBox modalTripFormSection;

    @FXML
    private VBox modalTaxRateFormSection;

    @FXML
    private VBox modalDeleteSection;

    @FXML
    private Label modalDeleteMessageLabel;

    @FXML
    private TextField modalNameField;

    @FXML
    private TextField modalEmailField;

    @FXML
    private TextField modalPhoneField;

    @FXML
    private Label modalReferenceLabel;

    @FXML
    private TextField modalReferenceField;

    @FXML
    private TextField modalTripClientIdField;

    @FXML
    private TextField modalTripDriverIdField;

    @FXML
    private ComboBox<TripType> modalTripTypeCombo;

    @FXML
    private ComboBox<TripStatus> modalTripStatusCombo;

    @FXML
    private TextField modalTripOriginField;

    @FXML
    private TextField modalTripDestinationField;

    @FXML
    private TextField modalTripEstimatedPriceField;

    @FXML
    private TextField modalTripFinalPriceField;

    @FXML
    private TextField modalTripNotesField;

    @FXML
    private ComboBox<AdminTaxRateDTO> modalTaxRateCombo;

    @FXML
    private TextField modalTaxRateNameField;

    @FXML
    private TextField modalTaxRateValueField;

    @FXML
    private TextField modalTaxRateDescriptionField;

    @FXML
    private CheckBox modalTaxRateActiveCheck;

    @FXML
    private ComboBox<AccountStatus> modalStatusCombo;

    @FXML
    private PasswordField modalPasswordField;

    @FXML
    private PasswordField modalConfirmPasswordField;

    @FXML
    private Button modalSaveButton;

    @FXML
    private Button modalDeleteConfirmButton;

    public AdminController(AdminService adminService) {
        if (adminService == null) {
            throw new IllegalArgumentException("AdminService must not be null.");
        }
        this.adminService = adminService;
    }

    @FXML
    public void initialize() {
        if (!SessionManager.hasRole(UserType.ADMIN)) {
            NavigationManager.navigateToDashboard();
            return;
        }

        updateSidebarUserCard();
        setupColumns();
        setupTripColumns();
        setupPaymentColumns();

        usersTable.setItems(filteredUsers);
        tripsTable.setItems(filteredTrips);
        paymentsTable.setItems(filteredPayments);
        usersTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, previous, current) -> onSelectionChanged(current));
        tripsTable.getSelectionModel().selectedItemProperty()
            .addListener((obs, previous, current) -> onTripSelectionChanged(current));
        paymentsTable.getSelectionModel().selectedItemProperty()
            .addListener((obs, previous, current) -> onPaymentSelectionChanged(current));
        searchField.textProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        modalStatusCombo.setItems(FXCollections.observableArrayList(AccountStatus.values()));
        modalTripTypeCombo.setItems(FXCollections.observableArrayList(TripType.values()));
        modalTripStatusCombo.setItems(FXCollections.observableArrayList(TripStatus.values()));
        periodComboBox.setItems(FXCollections.observableArrayList(AdminFinancialPeriod.values()));
        periodComboBox.setValue(currentFinancialPeriod);
        periodComboBox.setVisible(false);
        periodComboBox.setManaged(false);

        modalTaxRateCombo.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(AdminTaxRateDTO item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : formatTaxRateDisplay(item));
            }
        });
        modalTaxRateCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(AdminTaxRateDTO item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : formatTaxRateDisplay(item));
            }
        });
        modalTaxRateCombo.valueProperty().addListener((obs, oldValue, newValue) -> populateTaxRateFields(newValue));

        editUserButton.disableProperty().bind(usersTable.getSelectionModel().selectedItemProperty().isNull());
        deleteUserButton.disableProperty().bind(usersTable.getSelectionModel().selectedItemProperty().isNull());

        detailPanel.setManaged(false);
        detailPanel.setVisible(false);
        hideModal();

        switchSection(AdminSection.DRIVERS);
    }

    @FXML
    public void handleMotoristasSection() {
        switchSection(AdminSection.DRIVERS);
    }

    @FXML
    public void handleClientesSection() {
        switchSection(AdminSection.CLIENTS);
    }

    @FXML
    public void handleViagensSection() {
        switchSection(AdminSection.TRIPS);
    }

    @FXML
    public void handleFinanceiraSection() {
        switchSection(AdminSection.FINANCIAL);
    }

    @FXML
    public void handlePeriodChanged() {
        if (currentSection != AdminSection.FINANCIAL) {
            return;
        }
        currentFinancialPeriod = periodComboBox.getValue() == null ? AdminFinancialPeriod.MONTH : periodComboBox.getValue();
        refreshSectionData();
    }

    @FXML
    public void handleFinancialPeriodDay() {
        setFinancialPeriod(AdminFinancialPeriod.DAY);
    }

    @FXML
    public void handleFinancialPeriodWeek() {
        setFinancialPeriod(AdminFinancialPeriod.WEEK);
    }

    @FXML
    public void handleFinancialPeriodMonth() {
        setFinancialPeriod(AdminFinancialPeriod.MONTH);
    }

    @FXML
    public void handleFinancialPeriodYear() {
        setFinancialPeriod(AdminFinancialPeriod.YEAR);
    }

    @FXML
    public void handleFinancialPeriodAll() {
        setFinancialPeriod(AdminFinancialPeriod.ALL);
    }

    @FXML
    public void handleFilterAll() {
        if (currentSection == AdminSection.FINANCIAL) {
            setPaymentStatusFilter(null);
            return;
        }
        if (currentSection == AdminSection.TRIPS) {
            setTripStatusFilter(null);
            return;
        }
        setStatusFilter(null);
    }

    @FXML
    public void handleFilterActive() {
        if (currentSection == AdminSection.FINANCIAL) {
            setPaymentStatusFilter(PaymentStatus.PROCESSED);
            return;
        }
        if (currentSection == AdminSection.TRIPS) {
            setTripStatusFilter(TripStatus.ACCEPTED);
            return;
        }
        setStatusFilter(AccountStatus.ACTIVE);
    }

    @FXML
    public void handleFilterInactive() {
        if (currentSection == AdminSection.FINANCIAL) {
            setPaymentStatusFilter(PaymentStatus.FAILED);
            return;
        }
        if (currentSection == AdminSection.TRIPS) {
            setTripStatusFilter(TripStatus.IN_PROGRESS);
            return;
        }
        setStatusFilter(AccountStatus.INACTIVE);
    }

    @FXML
    public void handleFilterBlocked() {
        if (currentSection == AdminSection.FINANCIAL) {
            setPaymentStatusFilter(PaymentStatus.REFUNDED);
            return;
        }
        if (currentSection == AdminSection.TRIPS) {
            setTripStatusFilter(TripStatus.COMPLETED);
            return;
        }
        setStatusFilter(AccountStatus.BLOCKED);
    }

    @FXML
    public void handleFilterPending() {
        if (currentSection == AdminSection.FINANCIAL) {
            setPaymentStatusFilter(PaymentStatus.PENDING);
            return;
        }
        if (currentSection == AdminSection.TRIPS) {
            setTripStatusFilter(TripStatus.PENDING);
            return;
        }
        setStatusFilter(AccountStatus.PENDING);
    }

    @FXML
    public void handleExportSection(ActionEvent event) {
        if (currentSection != AdminSection.FINANCIAL) {
            int filteredCount = currentSection == AdminSection.TRIPS ? filteredTrips.size() : filteredUsers.size();
            showFeedback("Exportacao detalhada disponivel apenas na secao financeira. Registos filtrados: " + filteredCount, false);
            return;
        }

        try {
            FinancialExportSnapshot snapshot = buildFinancialExportSnapshot();
            Path exportDir = resolveExportDirectory();
            String timestamp = EXPORT_TIMESTAMP_FORMAT.format(snapshot.generatedAt());

            boolean exportPdf = event != null && event.getSource() == exportPdfButton;
            Path exportPath = exportDir.resolve("relatorio_financeiro_" + timestamp + (exportPdf ? ".pdf" : ".csv"));

            if (exportPdf) {
                exportFinancialPdf(snapshot, exportPath);
            } else {
                exportFinancialCsv(snapshot, exportPath);
            }

            showFeedback("Exportacao concluida: " + exportPath.toAbsolutePath(), false);
        } catch (Exception exception) {
            showFeedback("Falha na exportacao financeira: " + exception.getMessage(), true);
        }
    }

    private FinancialExportSnapshot buildFinancialExportSnapshot() {
        List<AdminPaymentByTripDTO> payments = new ArrayList<>(filteredPayments);
        AdminFinancialOverviewDTO overview = buildFilteredFinancialOverview(payments);
        List<AdminTaxRateDTO> taxRates = new ArrayList<>(allTaxRates);
        String scopeDescription = buildFinancialExportScopeDescription(payments.size(), allPayments.size());
        Map<String, Long> methods = groupPaymentMethods(payments);
        return new FinancialExportSnapshot(LocalDateTime.now(), scopeDescription, overview, payments, taxRates, methods);
    }

    private AdminFinancialOverviewDTO buildFilteredFinancialOverview(List<AdminPaymentByTripDTO> payments) {
        List<AdminPaymentByTripDTO> safePayments = payments == null ? List.of() : payments;
        EnumMap<PaymentStatus, Long> statusTotals = new EnumMap<>(PaymentStatus.class);
        for (PaymentStatus status : PaymentStatus.values()) {
            statusTotals.put(status, 0L);
        }

        BigDecimal processedIncome = BigDecimal.ZERO;
        for (AdminPaymentByTripDTO payment : safePayments) {
            PaymentStatus status = payment.getStatus();
            if (status != null) {
                statusTotals.put(status, statusTotals.get(status) + 1L);
            }
            if (status == PaymentStatus.PROCESSED && payment.getAmount() != null) {
                processedIncome = processedIncome.add(payment.getAmount());
            }
        }

        List<AdminPaymentStatusSummaryDTO> paymentStatuses = new ArrayList<>();
        for (PaymentStatus status : PaymentStatus.values()) {
            paymentStatuses.add(new AdminPaymentStatusSummaryDTO(status, statusTotals.get(status)));
        }

        return new AdminFinancialOverviewDTO(
                processedIncome,
                processedIncome,
                safePayments.size(),
                statusTotals.get(PaymentStatus.PENDING),
                statusTotals.get(PaymentStatus.PROCESSED),
                statusTotals.get(PaymentStatus.FAILED),
                statusTotals.get(PaymentStatus.REFUNDED),
                paymentStatuses);
    }

    private String buildFinancialExportScopeDescription(int filteredCount, int totalCount) {
        List<String> parts = new ArrayList<>();
        parts.add(prettyFinancialPeriod(currentFinancialPeriod));

        if (currentPaymentStatusFilter != null) {
            parts.add("Estado: " + prettyPaymentStatus(currentPaymentStatusFilter));
        }

        String rawQuery = searchField == null ? "" : (searchField.getText() == null ? "" : searchField.getText().trim());
        if (!rawQuery.isBlank()) {
            parts.add("Pesquisa: \"" + rawQuery + "\"");
        }

        parts.add(filteredCount + " de " + totalCount + " pagamentos");
        return String.join(" | ", parts);
    }

    private Path resolveExportDirectory() throws IOException {
        Path documentsDir = Path.of(System.getProperty("user.home"), "Documents", "obar-exports");
        Files.createDirectories(documentsDir);
        return documentsDir;
    }

    private void exportFinancialCsv(FinancialExportSnapshot snapshot, Path exportPath) throws IOException {
        try (BufferedWriter writer = Files.newBufferedWriter(exportPath, StandardCharsets.UTF_8)) {
            writer.write("Secao,Campo,Valor");
            writer.newLine();
            writer.write(csvLine("META", "Gerado em", EXPORT_READABLE_FORMAT.format(snapshot.generatedAt())));
            writer.newLine();
            writer.write(csvLine("META", "Periodo exportado", snapshot.scopeDescription()));
            writer.newLine();

            AdminFinancialOverviewDTO overview = snapshot.overview();
            writer.write(csvLine("RESUMO", "Receita total", formatCurrency(overview.getTotalIncome())));
            writer.newLine();
            writer.write(csvLine("RESUMO", "Receita periodo", formatCurrency(overview.getPeriodIncome())));
            writer.newLine();
            writer.write(csvLine("RESUMO", "Pagamentos totais", String.valueOf(overview.getTotalPayments())));
            writer.newLine();
            writer.write(csvLine("RESUMO", "Pagamentos processados", String.valueOf(overview.getProcessedPayments())));
            writer.newLine();
            writer.write(csvLine("RESUMO", "Pagamentos pendentes", String.valueOf(overview.getPendingPayments())));
            writer.newLine();
            writer.write(csvLine("RESUMO", "Pagamentos falhados", String.valueOf(overview.getFailedPayments())));
            writer.newLine();
            writer.write(csvLine("RESUMO", "Pagamentos reembolsados", String.valueOf(overview.getRefundedPayments())));
            writer.newLine();

            writer.newLine();
            writer.write("Estado,Total");
            writer.newLine();
            for (AdminPaymentStatusSummaryDTO status : overview.getPaymentStatuses()) {
                writer.write(csvLine(prettyPaymentStatus(status.getStatus()), String.valueOf(status.getTotal())));
                writer.newLine();
            }

            writer.newLine();
            writer.write("Metodo,Total,Percentagem");
            writer.newLine();
            long paymentCount = snapshot.payments().size();
            for (Map.Entry<String, Long> method : snapshot.paymentMethods().entrySet().stream()
                    .sorted((left, right) -> Long.compare(right.getValue(), left.getValue()))
                    .toList()) {
                double pct = paymentCount == 0 ? 0 : (method.getValue() * 100.0) / paymentCount;
                writer.write(csvLine(prettyPaymentMethod(method.getKey()), String.valueOf(method.getValue()), formatPercent(pct)));
                writer.newLine();
            }

            writer.newLine();
            writer.write("TaxaId,Nome,Valor,Descricao,Ativa");
            writer.newLine();
            for (AdminTaxRateDTO taxRate : snapshot.taxRates()) {
                writer.write(csvLine(
                        String.valueOf(taxRate.getId()),
                        fallback(taxRate.getName()),
                        taxRate.getRate() == null ? "-" : taxRate.getRate().toPlainString(),
                        fallback(taxRate.getDescription()),
                        Boolean.TRUE.equals(taxRate.getActive()) ? "Sim" : "Nao"));
                writer.newLine();
            }

            writer.newLine();
            writer.write("PagamentoId,ViagemId,Cliente,Motorista,Metodo,Valor,Moeda,Estado,Data,TaxaAplicada");
            writer.newLine();
            for (AdminPaymentByTripDTO payment : snapshot.payments()) {
                writer.write(csvLine(
                        payment.getPaymentId() == null ? "-" : String.valueOf(payment.getPaymentId()),
                        payment.getTripId() == null ? "-" : String.valueOf(payment.getTripId()),
                        fallback(payment.getClientName()),
                        fallback(payment.getDriverName()),
                        prettyPaymentMethod(payment.getPaymentMethodType()),
                        payment.getAmount() == null ? "0.00" : payment.getAmount().toPlainString(),
                        fallback(payment.getCurrencyCode()),
                        prettyPaymentStatus(payment.getStatus()),
                        payment.getPaymentDate() == null ? "-" : PAYMENT_DATE_FORMAT.format(payment.getPaymentDate()),
                        payment.getTaxRateApplied() == null ? "-" : payment.getTaxRateApplied().toPlainString()));
                writer.newLine();
            }
        }
    }

    private void exportFinancialPdf(FinancialExportSnapshot snapshot, Path exportPath) throws IOException, DocumentException {
        Document document = new Document(PageSize.A4.rotate(), 24, 24, 24, 24);
        PdfWriter.getInstance(document, new FileOutputStream(exportPath.toFile()));
        document.open();

        Font titleFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18);
        Font sectionFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12);
        Font bodyFont = FontFactory.getFont(FontFactory.HELVETICA, 10);

        Paragraph title = new Paragraph("OBAR - Relatorio Financeiro Completo", titleFont);
        title.setSpacingAfter(6f);
        document.add(title);
        document.add(new Paragraph("Gerado em: " + EXPORT_READABLE_FORMAT.format(snapshot.generatedAt()), bodyFont));
        document.add(new Paragraph("Periodo exportado: " + snapshot.scopeDescription(), bodyFont));
        document.add(new Paragraph(" "));

        Paragraph summarySectionTitle = new Paragraph("Resumo", sectionFont);
        summarySectionTitle.setSpacingAfter(8f);
        document.add(summarySectionTitle);
        PdfPTable summaryTable = new PdfPTable(new float[] { 3f, 2f, 2f, 2f, 2f, 2f, 2f });
        summaryTable.setWidthPercentage(100f);
        addPdfHeader(summaryTable, "Receita Total");
        addPdfHeader(summaryTable, "Receita Periodo");
        addPdfHeader(summaryTable, "Pagamentos");
        addPdfHeader(summaryTable, "Processados");
        addPdfHeader(summaryTable, "Pendentes");
        addPdfHeader(summaryTable, "Falhados");
        addPdfHeader(summaryTable, "Reembolsados");

        AdminFinancialOverviewDTO overview = snapshot.overview();
        addPdfCell(summaryTable, formatCurrency(overview.getTotalIncome()));
        addPdfCell(summaryTable, formatCurrency(overview.getPeriodIncome()));
        addPdfCell(summaryTable, String.valueOf(overview.getTotalPayments()));
        addPdfCell(summaryTable, String.valueOf(overview.getProcessedPayments()));
        addPdfCell(summaryTable, String.valueOf(overview.getPendingPayments()));
        addPdfCell(summaryTable, String.valueOf(overview.getFailedPayments()));
        addPdfCell(summaryTable, String.valueOf(overview.getRefundedPayments()));
        document.add(summaryTable);
        document.add(new Paragraph(" "));

        Paragraph methodsSectionTitle = new Paragraph("Distribuicao por Metodo", sectionFont);
        methodsSectionTitle.setSpacingAfter(8f);
        document.add(methodsSectionTitle);
        PdfPTable methodsTable = new PdfPTable(new float[] { 3f, 1f, 1f });
        methodsTable.setWidthPercentage(65f);
        addPdfHeader(methodsTable, "Metodo");
        addPdfHeader(methodsTable, "Total");
        addPdfHeader(methodsTable, "Percentagem");
        long paymentCount = snapshot.payments().size();
        for (Map.Entry<String, Long> method : snapshot.paymentMethods().entrySet().stream()
                .sorted((left, right) -> Long.compare(right.getValue(), left.getValue()))
                .toList()) {
            double pct = paymentCount == 0 ? 0 : (method.getValue() * 100.0) / paymentCount;
            addPdfCell(methodsTable, prettyPaymentMethod(method.getKey()));
            addPdfCell(methodsTable, String.valueOf(method.getValue()));
            addPdfCell(methodsTable, formatPercent(pct));
        }
        document.add(methodsTable);
        document.add(new Paragraph(" "));

        Paragraph paymentsSectionTitle = new Paragraph("Pagamentos", sectionFont);
        paymentsSectionTitle.setSpacingAfter(8f);
        document.add(paymentsSectionTitle);
        PdfPTable paymentsTablePdf = new PdfPTable(new float[] { 1.1f, 1.1f, 2.1f, 2.1f, 1.6f, 1.3f, 0.9f, 1.2f, 1.6f, 1.1f });
        paymentsTablePdf.setWidthPercentage(100f);
        addPdfHeader(paymentsTablePdf, "Pagamento");
        addPdfHeader(paymentsTablePdf, "Viagem");
        addPdfHeader(paymentsTablePdf, "Cliente");
        addPdfHeader(paymentsTablePdf, "Motorista");
        addPdfHeader(paymentsTablePdf, "Metodo");
        addPdfHeader(paymentsTablePdf, "Valor");
        addPdfHeader(paymentsTablePdf, "Moeda");
        addPdfHeader(paymentsTablePdf, "Estado");
        addPdfHeader(paymentsTablePdf, "Data");
        addPdfHeader(paymentsTablePdf, "Taxa");

        for (AdminPaymentByTripDTO payment : snapshot.payments()) {
            addPdfCell(paymentsTablePdf, payment.getPaymentId() == null ? "-" : "#" + payment.getPaymentId());
            addPdfCell(paymentsTablePdf, payment.getTripId() == null ? "-" : "#" + payment.getTripId());
            addPdfCell(paymentsTablePdf, fallback(payment.getClientName()));
            addPdfCell(paymentsTablePdf, fallback(payment.getDriverName()));
            addPdfCell(paymentsTablePdf, prettyPaymentMethod(payment.getPaymentMethodType()));
            addPdfCell(paymentsTablePdf, payment.getAmount() == null ? "0.00" : payment.getAmount().toPlainString());
            addPdfCell(paymentsTablePdf, fallback(payment.getCurrencyCode()));
            addPdfCell(paymentsTablePdf, prettyPaymentStatus(payment.getStatus()));
            addPdfCell(paymentsTablePdf, payment.getPaymentDate() == null ? "-" : PAYMENT_DATE_FORMAT.format(payment.getPaymentDate()));
            addPdfCell(paymentsTablePdf, payment.getTaxRateApplied() == null ? "-" : payment.getTaxRateApplied().toPlainString());
        }
        document.add(paymentsTablePdf);

        document.add(new Paragraph(" "));
        Paragraph taxSectionTitle = new Paragraph("Taxas de IVA", sectionFont);
        taxSectionTitle.setSpacingAfter(8f);
        document.add(taxSectionTitle);
        PdfPTable taxTable = new PdfPTable(new float[] { 0.8f, 2f, 1.2f, 3f, 0.9f });
        taxTable.setWidthPercentage(100f);
        addPdfHeader(taxTable, "ID");
        addPdfHeader(taxTable, "Nome");
        addPdfHeader(taxTable, "Valor");
        addPdfHeader(taxTable, "Descricao");
        addPdfHeader(taxTable, "Ativa");
        for (AdminTaxRateDTO taxRate : snapshot.taxRates()) {
            addPdfCell(taxTable, taxRate.getId() == null ? "-" : String.valueOf(taxRate.getId()));
            addPdfCell(taxTable, fallback(taxRate.getName()));
            addPdfCell(taxTable, taxRate.getRate() == null ? "-" : taxRate.getRate().toPlainString());
            addPdfCell(taxTable, fallback(taxRate.getDescription()));
            addPdfCell(taxTable, Boolean.TRUE.equals(taxRate.getActive()) ? "Sim" : "Nao");
        }
        document.add(taxTable);

        document.close();
    }

    private void addPdfHeader(PdfPTable table, String value) {
        PdfPCell cell = new PdfPCell(new Phrase(value, FontFactory.getFont(FontFactory.HELVETICA_BOLD, 9)));
        cell.setBackgroundColor(new java.awt.Color(230, 234, 242));
        cell.setPadding(6f);
        table.addCell(cell);
    }

    private void addPdfCell(PdfPTable table, String value) {
        PdfPCell cell = new PdfPCell(new Phrase(value == null ? "-" : value, FontFactory.getFont(FontFactory.HELVETICA, 9)));
        cell.setPadding(5f);
        table.addCell(cell);
    }

    private String csvLine(String... values) {
        return java.util.Arrays.stream(values)
                .map(this::escapeCsv)
                .collect(Collectors.joining(","));
    }

    private String escapeCsv(String value) {
        String safe = value == null ? "" : value;
        boolean quote = safe.contains(",") || safe.contains("\"") || safe.contains("\n") || safe.contains("\r");
        if (quote) {
            return "\"" + safe.replace("\"", "\"\"") + "\"";
        }
        return safe;
    }

    @FXML
    public void handleAddUser() {
        if (currentSection == AdminSection.FINANCIAL) {
            showFeedback("Nesta secao so e permitido editar taxas de IVA.", false);
            return;
        }
        if (currentSection == AdminSection.TRIPS) {
            openTripFormModal();
            return;
        }
        openUserFormModal(null);
    }

    @FXML
    public void handleEditUser() {
        if (currentSection == AdminSection.FINANCIAL) {
            openTaxRateEditModal();
            return;
        }
        if (currentSection == AdminSection.TRIPS) {
            AdminTripDTO selectedTrip = tripsTable.getSelectionModel().getSelectedItem();
            if (selectedTrip == null) {
                showFeedback("Selecione uma viagem primeiro.", true);
                return;
            }
            openTripFormModal(selectedTrip);
            return;
        }
        AdminUserDTO selectedUser = usersTable.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            showFeedback("Selecione um registo primeiro.", true);
            return;
        }

        if (selectedUser.getType() != currentSection.userType) {
            showFeedback("O registo selecionado nao pertence a esta secao.", true);
            return;
        }

        openUserFormModal(selectedUser);
    }

    @FXML
    public void handleDeleteUser() {
        if (currentSection == AdminSection.FINANCIAL) {
            showFeedback("Remocao de dados financeiros nao esta disponivel nesta vista.", true);
            return;
        }
        if (currentSection == AdminSection.TRIPS) {
            AdminTripDTO selectedTrip = tripsTable.getSelectionModel().getSelectedItem();
            if (selectedTrip == null) {
                showFeedback("Selecione uma viagem primeiro.", true);
                return;
            }
            openDeleteTripConfirmModal(selectedTrip);
            return;
        }
        AdminUserDTO selectedUser = usersTable.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            showFeedback("Selecione um registo primeiro.", true);
            return;
        }

        openDeleteConfirmModal(selectedUser);
    }

    @FXML
    public void handleModalCancel() {
        hideModal();
    }

    @FXML
    public void handleModalSave() {
        if (modalMode != ModalMode.CREATE
                && modalMode != ModalMode.EDIT
                && modalMode != ModalMode.CREATE_TRIP
                && modalMode != ModalMode.EDIT_TRIP
                && modalMode != ModalMode.EDIT_TAX_RATE) {
            hideModal();
            return;
        }

        if (modalMode == ModalMode.EDIT_TAX_RATE) {
            persistTaxRateUpdate();
            return;
        }

        if (modalMode == ModalMode.CREATE_TRIP || modalMode == ModalMode.EDIT_TRIP) {
            persistTrip();
            return;
        }

        boolean editing = modalMode == ModalMode.EDIT;
        String password = modalPasswordField.getText();
        String confirmPassword = modalConfirmPasswordField.getText();
        if ((password == null ? "" : password).equals(confirmPassword == null ? "" : confirmPassword)) {
            if (editing) {
                persistEditUser();
            } else {
                persistCreateUser();
            }
            return;
        }

        showModalError("Password e confirmacao nao coincidem.");
    }

    @FXML
    public void handleModalConfirmDelete() {
        if (modalMode != ModalMode.DELETE_CONFIRM || modalTargetUser == null) {
            if (modalMode == ModalMode.DELETE_TRIP_CONFIRM && modalTargetTrip != null) {
                try {
                    adminService.deleteTrip(modalTargetTrip.getId());
                    showFeedback("Viagem apagada com sucesso.", false);
                    hideModal();
                    refreshSectionData();
                } catch (Exception exception) {
                    showModalError("Falha ao apagar viagem: " + exception.getMessage());
                }
                return;
            }
            hideModal();
            return;
        }

        try {
            boolean deleted = adminService.blockOrDeleteUser(modalTargetUser.getId());
            if (deleted) {
                showFeedback("Registo removido com sucesso.", false);
            } else {
                showFeedback("Conta bloqueada com sucesso.", false);
            }

            hideModal();
            refreshSectionData();
        } catch (Exception exception) {
            showModalError("Falha ao atualizar registo. O utilizador pode ter referencias ativas.");
        }
    }

    @FXML
    public void handleBackToDashboard() {
        NavigationManager.navigateToDashboard();
    }

    @FXML
    public void handleCloseDetailPanel() {
        if (currentSection == AdminSection.TRIPS) {
            tripsTable.getSelectionModel().clearSelection();
        } else if (currentSection == AdminSection.FINANCIAL) {
            paymentsTable.getSelectionModel().clearSelection();
        } else {
            usersTable.getSelectionModel().clearSelection();
        }

        detailPanel.setVisible(false);
        detailPanel.setManaged(false);
    }

    @FXML
    public void handleLogout() {
        SessionManager.logout();
        NavigationManager.navigateToLogin();
    }

    private void setupColumns() {
        userIdColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(cellData.getValue().getId() == null
                ? "-"
                : "#" + cellData.getValue().getId()));
        nameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(cellData.getValue().getName()));
        emailColumn.setCellValueFactory(cellData -> new SimpleStringProperty(fallback(cellData.getValue().getEmail())));

        statusColumn.setCellValueFactory(
                cellData -> new SimpleStringProperty(prettyStatus(cellData.getValue().getStatus())));
        statusColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("status-online", "status-offline", "status-blocked", "status-pending");

                if (empty || item == null) {
                    setText(null);
                    return;
                }

                setText(item);
                if (getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    return;
                }
                AdminUserDTO rowUser = getTableView().getItems().get(getIndex());
                if (rowUser.getStatus() == AccountStatus.ACTIVE) {
                    getStyleClass().add("status-online");
                } else if (rowUser.getStatus() == AccountStatus.INACTIVE) {
                    getStyleClass().add("status-offline");
                } else if (rowUser.getStatus() == AccountStatus.BLOCKED) {
                    getStyleClass().add("status-blocked");
                } else if (rowUser.getStatus() == AccountStatus.PENDING) {
                    getStyleClass().add("status-pending");
                }
            }
        });

        metricColumn.setCellValueFactory(cellData -> new SimpleStringProperty(metricValue(cellData.getValue())));
        volumeColumn.setCellValueFactory(cellData -> new SimpleStringProperty(volumeValue(cellData.getValue())));
        referenceColumn.setCellValueFactory(cellData -> new SimpleStringProperty(referenceValue(cellData.getValue())));
        createdAtColumn.setCellValueFactory(cellData -> {
            LocalDateTime createdAt = cellData.getValue().getCreatedAt();
            return new SimpleStringProperty(createdAt == null ? "-" : TABLE_CREATED_AT_FORMAT.format(createdAt));
        });
    }

    private void setupTripColumns() {
        tripIdColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(cellData.getValue().getId() == null
                        ? "-"
                        : "#" + cellData.getValue().getId()));
        tripClientColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(fallback(cellData.getValue().getClientName())));
        tripDriverColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(fallback(cellData.getValue().getDriverName())));
        tripTypeColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(prettyTripType(cellData.getValue().getTripType())));
        tripPriceColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatTripPrice(cellData.getValue())));
        tripRequestedAtColumn.setCellValueFactory(cellData -> {
            LocalDateTime requestTime = cellData.getValue().getRequestTime();
            return new SimpleStringProperty(requestTime == null ? "-" : TRIP_REQUESTED_AT_FORMAT.format(requestTime));
        });

        tripStatusColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(prettyTripStatus(cellData.getValue().getStatus())));
        tripStatusColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll(
                        "trip-completed",
                        "trip-in-progress",
                        "trip-accepted",
                        "trip-pending",
                        "trip-cancelled",
                        "trip-rejected");

                if (empty || item == null) {
                    setText(null);
                    return;
                }

                setText(item);
                if (getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    return;
                }

                AdminTripDTO rowTrip = getTableView().getItems().get(getIndex());
                if (rowTrip.getStatus() == null) {
                    return;
                }

                switch (rowTrip.getStatus()) {
                    case COMPLETED -> getStyleClass().add("trip-completed");
                    case IN_PROGRESS -> getStyleClass().add("trip-in-progress");
                    case ACCEPTED -> getStyleClass().add("trip-accepted");
                    case PENDING -> getStyleClass().add("trip-pending");
                    case CANCELLED -> getStyleClass().add("trip-cancelled");
                    case REJECTED -> getStyleClass().add("trip-rejected");
                }
            }
        });
    }

    private void setupPaymentColumns() {
        paymentIdColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getPaymentId() == null ? "-" : "#" + cellData.getValue().getPaymentId()));
        paymentTripIdColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getTripId() == null ? "-" : "#" + cellData.getValue().getTripId()));
        paymentClientColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(fallback(cellData.getValue().getClientName())));
        paymentMethodColumn.setCellValueFactory(cellData ->
            new SimpleStringProperty(prettyPaymentMethod(cellData.getValue().getPaymentMethodType())));
        paymentAmountColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(formatPaymentAmount(cellData.getValue())));
        paymentDateColumn.setCellValueFactory(cellData -> {
            LocalDateTime paymentDate = cellData.getValue().getPaymentDate();
            return new SimpleStringProperty(paymentDate == null ? "-" : PAYMENT_DATE_FORMAT.format(paymentDate));
        });

        paymentStatusColumn.setCellValueFactory(cellData ->
                new SimpleStringProperty(prettyPaymentStatus(cellData.getValue().getStatus())));
        paymentStatusColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll(
                        "payment-processed",
                        "payment-pending",
                        "payment-failed",
                        "payment-refunded");

                if (empty || item == null) {
                    setText(null);
                    return;
                }

                setText(item);
                if (getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    return;
                }

                AdminPaymentByTripDTO rowPayment = getTableView().getItems().get(getIndex());
                if (rowPayment.getStatus() == null) {
                    return;
                }

                switch (rowPayment.getStatus()) {
                    case PROCESSED -> getStyleClass().add("payment-processed");
                    case PENDING -> getStyleClass().add("payment-pending");
                    case FAILED -> getStyleClass().add("payment-failed");
                    case REFUNDED -> getStyleClass().add("payment-refunded");
                }
            }
        });
    }

    private void switchSection(AdminSection section) {
        this.currentSection = section;
        currentSectionLabel.setText(section.title);
        addUserButton.setText(section.createLabel);

        boolean isTripsSection = section == AdminSection.TRIPS;
        boolean isFinancialSection = section == AdminSection.FINANCIAL;
        usersTable.setVisible(!isTripsSection && !isFinancialSection);
        usersTable.setManaged(!isTripsSection && !isFinancialSection);
        tripsTable.setVisible(isTripsSection);
        tripsTable.setManaged(isTripsSection);
        paymentsTable.setVisible(isFinancialSection);
        paymentsTable.setManaged(isFinancialSection);
        financialDashboard.setVisible(isFinancialSection);
        financialDashboard.setManaged(isFinancialSection);
        paymentsTableTitle.setVisible(isFinancialSection);
        paymentsTableTitle.setManaged(isFinancialSection);
        periodComboBox.setVisible(isFinancialSection);
        periodComboBox.setManaged(isFinancialSection);
        searchFilterRow.setVisible(!isFinancialSection);
        searchFilterRow.setManaged(!isFinancialSection);
        tableActionRow.setVisible(!isFinancialSection);
        tableActionRow.setManaged(!isFinancialSection);
        exportPdfButton.setVisible(isFinancialSection);
        exportPdfButton.setManaged(isFinancialSection);
        exportCsvButton.setText(isFinancialSection ? "Exportar CSV" : "Exportar");

        addUserButton.setVisible(!isFinancialSection);
        addUserButton.setManaged(!isFinancialSection);
        editUserButton.setVisible(true);
        editUserButton.setManaged(true);
        deleteUserButton.setVisible(!isFinancialSection);
        deleteUserButton.setManaged(!isFinancialSection);
        filterAllButton.setVisible(true);
        filterAllButton.setManaged(true);
        filterActiveButton.setVisible(true);
        filterActiveButton.setManaged(true);
        filterInactiveButton.setVisible(true);
        filterInactiveButton.setManaged(true);
        filterBlockedButton.setVisible(true);
        filterBlockedButton.setManaged(true);
        filterPendingButton.setVisible(true);
        filterPendingButton.setManaged(true);

        boolean hasUserSelection = usersTable.getSelectionModel().getSelectedItem() != null;
        boolean hasTripSelection = tripsTable.getSelectionModel().getSelectedItem() != null;
        boolean hasPaymentSelection = paymentsTable.getSelectionModel().getSelectedItem() != null;
        boolean showDetails = isFinancialSection ? hasPaymentSelection : (isTripsSection ? hasTripSelection : hasUserSelection);
        detailPanel.setVisible(showDetails);
        detailPanel.setManaged(showDetails);

        if (section == AdminSection.DRIVERS) {
            nameColumn.setText("Motorista");
            emailColumn.setText("Email");
            emailColumn.setVisible(true);
            metricColumn.setText("Avaliacao");
            volumeColumn.setText("Viagens");
            referenceColumn.setText("Licenca");
            editUserButton.setText("Editar");
            deleteUserButton.setText("Bloquear/Apagar");
            searchField.setPromptText("Pesquisar por nome, email, telefone, licenca...");
        } else if (section == AdminSection.CLIENTS) {
            nameColumn.setText("Cliente");
            emailColumn.setVisible(false);
            metricColumn.setText("Email");
            volumeColumn.setText("Telefone");
            referenceColumn.setText("NIF");
            editUserButton.setText("Editar");
            deleteUserButton.setText("Bloquear/Apagar");
            searchField.setPromptText("Pesquisar por nome, email, telefone, NIF...");
        } else if (section == AdminSection.TRIPS) {
            editUserButton.setText("Editar");
            deleteUserButton.setText("Apagar");
            editUserButton.disableProperty().unbind();
            deleteUserButton.disableProperty().unbind();
            editUserButton.disableProperty().bind(tripsTable.getSelectionModel().selectedItemProperty().isNull());
            deleteUserButton.disableProperty().bind(tripsTable.getSelectionModel().selectedItemProperty().isNull());
            searchField.setPromptText("Pesquisar por ID, cliente, motorista, estado...");
        } else {
            editUserButton.setText("Editar IVA");
            editUserButton.disableProperty().unbind();
            editUserButton.setDisable(false);
            deleteUserButton.disableProperty().unbind();
            deleteUserButton.setDisable(true);
            searchField.setPromptText("Pesquisar por pagamento, viagem, cliente, motorista...");
        }

        updateFinancialPeriodButtons();

        if (!isTripsSection && !isFinancialSection) {
            editUserButton.disableProperty().unbind();
            deleteUserButton.disableProperty().unbind();
            editUserButton.disableProperty().bind(usersTable.getSelectionModel().selectedItemProperty().isNull());
            deleteUserButton.disableProperty().bind(usersTable.getSelectionModel().selectedItemProperty().isNull());
        }

        updateSectionButtonState();
        if (isFinancialSection) {
            setPaymentStatusFilter(null);
        } else if (isTripsSection) {
            setTripStatusFilter(null);
        } else {
            setStatusFilter(null);
        }
        refreshSectionData();
        hideModal();
    }

    private void refreshSectionData() {
        if (currentSection == AdminSection.TRIPS) {
            allTrips.setAll(adminService.listTrips());
        } else if (currentSection == AdminSection.FINANCIAL) {
            allPayments.setAll(adminService.listPaymentsByTrip(currentFinancialPeriod));
            allTaxRates.setAll(adminService.listTaxRates());
            currentFinancialOverview = adminService.getFinancialOverview(currentFinancialPeriod);
            updateFinancialDetailPanel();
            updateFinancialDashboard();
        } else {
            allUsers.setAll(adminService.listUsersByType(currentSection.userType));
        }
        applyFilters();
        updateFilterLabels();
        usersTable.getSelectionModel().clearSelection();
        tripsTable.getSelectionModel().clearSelection();
        paymentsTable.getSelectionModel().clearSelection();
        clearDetailPanel();
        if (currentSection == AdminSection.FINANCIAL) {
            updateFinancialDetailPanel();
            updateFinancialDashboard();
        }
    }

    private void onSelectionChanged(AdminUserDTO selectedUser) {
        if (currentSection == AdminSection.TRIPS || currentSection == AdminSection.FINANCIAL) {
            return;
        }

        boolean hasSelection = selectedUser != null;
        detailPanel.setVisible(hasSelection);
        detailPanel.setManaged(hasSelection);

        if (hasSelection) {
            updateDetailsPanel(selectedUser);
        } else {
            clearDetailPanel();
        }
    }

    private void onTripSelectionChanged(AdminTripDTO selectedTrip) {
        if (currentSection != AdminSection.TRIPS) {
            return;
        }

        boolean hasSelection = selectedTrip != null;
        detailPanel.setVisible(hasSelection);
        detailPanel.setManaged(hasSelection);

        if (hasSelection) {
            updateTripDetailsPanel(selectedTrip);
        } else {
            clearDetailPanel();
        }
    }

    private void onPaymentSelectionChanged(AdminPaymentByTripDTO selectedPayment) {
        if (currentSection != AdminSection.FINANCIAL) {
            return;
        }

        boolean hasSelection = selectedPayment != null;
        detailPanel.setVisible(hasSelection);
        detailPanel.setManaged(hasSelection);

        if (hasSelection) {
            updatePaymentDetailsPanel(selectedPayment);
        } else {
            clearDetailPanel();
        }
    }

    private void setStatusFilter(AccountStatus status) {
        this.currentStatusFilter = status;
        updateFilterButtonState();
        applyFilters();
    }

    private void setTripStatusFilter(TripStatus status) {
        this.currentTripStatusFilter = status;
        updateFilterButtonState();
        applyFilters();
    }

    private void setPaymentStatusFilter(PaymentStatus status) {
        this.currentPaymentStatusFilter = status;
        updateFilterButtonState();
        applyFilters();
    }

    private void setFinancialPeriod(AdminFinancialPeriod period) {
        if (currentSection != AdminSection.FINANCIAL || period == null || period == currentFinancialPeriod) {
            return;
        }

        currentFinancialPeriod = period;
        periodComboBox.setValue(period);
        refreshSectionData();
    }

    private void applyFilters() {
        String query = normalize(searchField.getText());
        if (currentSection == AdminSection.TRIPS) {
            filteredTrips.setPredicate(trip -> matchesTripStatus(trip) && matchesTripQuery(trip, query));
        } else if (currentSection == AdminSection.FINANCIAL) {
            filteredPayments.setPredicate(payment -> matchesPaymentStatus(payment) && matchesPaymentQuery(payment, query));
        } else {
            filteredUsers.setPredicate(user -> matchesStatus(user) && matchesQuery(user, query));
        }
        updateListInfo();
    }

    private void updateListInfo() {
        if (currentSection == AdminSection.TRIPS) {
            listInfoLabel.setText("A mostrar " + filteredTrips.size() + " de " + allTrips.size() + " viagens");
            return;
        }
        if (currentSection == AdminSection.FINANCIAL) {
            listInfoLabel.setText("A mostrar " + filteredPayments.size() + " de " + allPayments.size() + " pagamentos");
            return;
        }

        listInfoLabel.setText("A mostrar " + filteredUsers.size() + " de " + allUsers.size() + " "
                + currentSection.title.toLowerCase(Locale.ROOT));
    }

    private boolean matchesStatus(AdminUserDTO user) {
        return currentStatusFilter == null || user.getStatus() == currentStatusFilter;
    }

    private boolean matchesTripStatus(AdminTripDTO trip) {
        return currentTripStatusFilter == null || trip.getStatus() == currentTripStatusFilter;
    }

    private boolean matchesPaymentStatus(AdminPaymentByTripDTO payment) {
        return currentPaymentStatusFilter == null || payment.getStatus() == currentPaymentStatusFilter;
    }

    private boolean matchesQuery(AdminUserDTO user, String query) {
        if (query.isBlank()) {
            return true;
        }

        return normalize(user.getName()).contains(query)
                || normalize(user.getEmail()).contains(query)
                || normalize(user.getPhone()).contains(query)
                || normalize(user.getLicenseNumber()).contains(query)
                || normalize(user.getTaxNumber()).contains(query);
    }

    private boolean matchesTripQuery(AdminTripDTO trip, String query) {
        if (query.isBlank()) {
            return true;
        }

        String idValue = trip.getId() == null ? "" : String.valueOf(trip.getId());
        return normalize(idValue).contains(query)
                || normalize(trip.getClientName()).contains(query)
                || normalize(trip.getDriverName()).contains(query)
                || normalize(prettyTripStatus(trip.getStatus())).contains(query)
                || normalize(prettyTripType(trip.getTripType())).contains(query)
                || normalize(trip.getOriginAddress()).contains(query)
                || normalize(trip.getDestinationAddress()).contains(query);
    }

    private boolean matchesPaymentQuery(AdminPaymentByTripDTO payment, String query) {
        if (query.isBlank()) {
            return true;
        }

        String paymentIdValue = payment.getPaymentId() == null ? "" : String.valueOf(payment.getPaymentId());
        String tripIdValue = payment.getTripId() == null ? "" : String.valueOf(payment.getTripId());
        return normalize(paymentIdValue).contains(query)
                || normalize(tripIdValue).contains(query)
                || normalize(payment.getClientName()).contains(query)
                || normalize(payment.getDriverName()).contains(query)
            || normalize(prettyPaymentMethod(payment.getPaymentMethodType())).contains(query)
                || normalize(prettyPaymentStatus(payment.getStatus())).contains(query);
    }

    private void updateSectionButtonState() {
        setButtonState(motoristasSectionButton, currentSection == AdminSection.DRIVERS, "sidebar-item",
                "sidebar-item-active");
        setButtonState(clientesSectionButton, currentSection == AdminSection.CLIENTS, "sidebar-item",
                "sidebar-item-active");
        setButtonState(viagensSectionButton, currentSection == AdminSection.TRIPS, "sidebar-item",
                "sidebar-item-active");
        setButtonState(financeiraSectionButton, currentSection == AdminSection.FINANCIAL, "sidebar-item",
            "sidebar-item-active");
    }

    private void updateFilterButtonState() {
        if (currentSection == AdminSection.FINANCIAL) {
            setButtonState(filterAllButton, currentPaymentStatusFilter == null, "filter-chip", "filter-chip-active");
            setButtonState(filterActiveButton, currentPaymentStatusFilter == PaymentStatus.PROCESSED, "filter-chip",
                "filter-chip-active");
            setButtonState(filterInactiveButton, currentPaymentStatusFilter == PaymentStatus.FAILED, "filter-chip",
                "filter-chip-active");
            setButtonState(filterBlockedButton, currentPaymentStatusFilter == PaymentStatus.REFUNDED, "filter-chip",
                "filter-chip-active");
            setButtonState(filterPendingButton, currentPaymentStatusFilter == PaymentStatus.PENDING, "filter-chip",
                "filter-chip-active");
            return;
        }
        if (currentSection == AdminSection.TRIPS) {
            setButtonState(filterAllButton, currentTripStatusFilter == null, "filter-chip", "filter-chip-active");
            setButtonState(filterActiveButton, currentTripStatusFilter == TripStatus.ACCEPTED, "filter-chip",
                "filter-chip-active");
            setButtonState(filterInactiveButton, currentTripStatusFilter == TripStatus.IN_PROGRESS, "filter-chip",
                "filter-chip-active");
            setButtonState(filterBlockedButton, currentTripStatusFilter == TripStatus.COMPLETED, "filter-chip",
                "filter-chip-active");
            setButtonState(filterPendingButton, currentTripStatusFilter == TripStatus.PENDING, "filter-chip",
                "filter-chip-active");
            return;
        }

        setButtonState(filterAllButton, currentStatusFilter == null, "filter-chip", "filter-chip-active");
        setButtonState(filterActiveButton, currentStatusFilter == AccountStatus.ACTIVE, "filter-chip",
            "filter-chip-active");
        setButtonState(filterInactiveButton, currentStatusFilter == AccountStatus.INACTIVE, "filter-chip",
            "filter-chip-active");
        setButtonState(filterBlockedButton, currentStatusFilter == AccountStatus.BLOCKED, "filter-chip",
            "filter-chip-active");
        setButtonState(filterPendingButton, currentStatusFilter == AccountStatus.PENDING, "filter-chip",
            "filter-chip-active");
    }

    private void setButtonState(Button button, boolean active, String baseClass, String activeClass) {
        button.getStyleClass().removeAll(baseClass, activeClass);
        button.getStyleClass().add(active ? activeClass : baseClass);
    }

    private void updateFilterLabels() {
        if (currentSection == AdminSection.FINANCIAL) {
            long pendingCount = allPayments.stream().filter(payment -> payment.getStatus() == PaymentStatus.PENDING).count();
            long processedCount = allPayments.stream().filter(payment -> payment.getStatus() == PaymentStatus.PROCESSED)
                    .count();
            long failedCount = allPayments.stream().filter(payment -> payment.getStatus() == PaymentStatus.FAILED).count();
            long refundedCount = allPayments.stream().filter(payment -> payment.getStatus() == PaymentStatus.REFUNDED)
                    .count();

            filterAllButton.setText("Todos (" + allPayments.size() + ")");
            filterActiveButton.setText("Processados (" + processedCount + ")");
            filterInactiveButton.setText("Falhados (" + failedCount + ")");
            filterBlockedButton.setText("Reembolsados (" + refundedCount + ")");
            filterPendingButton.setText("Pendentes (" + pendingCount + ")");
            return;
        }
        if (currentSection == AdminSection.TRIPS) {
            long pendingCount = allTrips.stream().filter(trip -> trip.getStatus() == TripStatus.PENDING).count();
            long acceptedCount = allTrips.stream().filter(trip -> trip.getStatus() == TripStatus.ACCEPTED).count();
            long inProgressCount = allTrips.stream().filter(trip -> trip.getStatus() == TripStatus.IN_PROGRESS).count();
            long completedCount = allTrips.stream().filter(trip -> trip.getStatus() == TripStatus.COMPLETED).count();

            filterAllButton.setText("Todos (" + allTrips.size() + ")");
            filterActiveButton.setText("Aceites (" + acceptedCount + ")");
            filterInactiveButton.setText("Em progresso (" + inProgressCount + ")");
            filterBlockedButton.setText("Concluidas (" + completedCount + ")");
            filterPendingButton.setText("Pendentes (" + pendingCount + ")");
            return;
        }

        long activeCount = allUsers.stream().filter(user -> user.getStatus() == AccountStatus.ACTIVE).count();
        long inactiveCount = allUsers.stream().filter(user -> user.getStatus() == AccountStatus.INACTIVE).count();
        long blockedCount = allUsers.stream().filter(user -> user.getStatus() == AccountStatus.BLOCKED).count();
        long pendingCount = allUsers.stream().filter(user -> user.getStatus() == AccountStatus.PENDING).count();

        filterAllButton.setText("Todos (" + allUsers.size() + ")");
        filterActiveButton.setText("Ativos (" + activeCount + ")");
        filterInactiveButton.setText("Offline (" + inactiveCount + ")");
        filterBlockedButton.setText("Bloqueados (" + blockedCount + ")");
        filterPendingButton.setText("Pendentes (" + pendingCount + ")");
    }

    private void updateSidebarUserCard() {
        AuthenticatedUserDto currentUser = SessionManager.getCurrentUser();
        sidebarUserInitialsLabel.setText(extractInitials(currentUser.name()));
        sidebarUserNameLabel.setText(fallback(currentUser.name()));
        sidebarUserRoleLabel.setText(prettyUserType(currentUser.type()));
    }

    private void updateTripDetailsPanel(AdminTripDTO trip) {
        detailTitleLabel.setText("Detalhe da viagem");
        detailInitialsLabel.setText(trip.getId() == null ? "--" : "#" + trip.getId());
        detailNameLabel.setText(fallback(trip.getOriginAddress()) + " -> " + fallback(trip.getDestinationAddress()));
        detailEmailLabel.setText("Cliente: " + fallback(trip.getClientName()));
        detailStatusLabel.setText(prettyTripStatus(trip.getStatus()));
        detailRoleLabel.setText(prettyTripType(trip.getTripType()));
        detailPhoneValueLabel.setText("Motorista: " + fallback(trip.getDriverName()));
        detailCreatedValueLabel.setText(trip.getRequestTime() == null ? "-" : DETAIL_CREATED_AT_FORMAT.format(trip.getRequestTime()));

        detailCardOneTitleLabel.setText("ID Cliente");
        detailCardOneValueLabel.setText(trip.getClientId() == null ? "-" : "#" + trip.getClientId());
        detailCardTwoTitleLabel.setText("ID Motorista");
        detailCardTwoValueLabel.setText(trip.getDriverId() == null ? "-" : "#" + trip.getDriverId());
        detailCardThreeTitleLabel.setText("Veiculo");
        detailCardThreeValueLabel.setText(fallback(trip.getVehicleDisplay()));
        detailCardFourTitleLabel.setText("Distancia");
        detailCardFourValueLabel.setText(trip.getDistanceKm() == null ? "-" : trip.getDistanceKm() + " km");

        detailReferenceTitleLabel.setText("Preco estimado");
        detailReferenceValueLabel.setText(trip.getEstimatedPrice() == null ? "-" : "EUR " + trip.getEstimatedPrice());

        detailExtraOneTitleLabel.setText("Preco final");
        detailExtraOneValueLabel.setText(trip.getFinalPrice() == null ? "-" : "EUR " + trip.getFinalPrice());
        detailExtraTwoTitleLabel.setText("Inicio");
        detailExtraTwoValueLabel.setText(trip.getStartTime() == null ? "-" : DETAIL_CREATED_AT_FORMAT.format(trip.getStartTime()));
        detailExtraThreeTitleLabel.setText("Fim");
        detailExtraThreeValueLabel.setText(trip.getEndTime() == null ? "-" : DETAIL_CREATED_AT_FORMAT.format(trip.getEndTime()));
    }

    private void updatePaymentDetailsPanel(AdminPaymentByTripDTO payment) {
        detailTitleLabel.setText("Detalhe do pagamento");
        detailInitialsLabel.setText(payment.getPaymentId() == null ? "--" : "#" + payment.getPaymentId());
        detailNameLabel.setText(formatPaymentAmount(payment));
        detailEmailLabel.setText("Cliente: " + fallback(payment.getClientName()));
        detailStatusLabel.setText(prettyPaymentStatus(payment.getStatus()));
        detailRoleLabel.setText(prettyPaymentMethod(payment.getPaymentMethodType()));
        detailPhoneValueLabel.setText("Metodo: " + prettyPaymentMethod(payment.getPaymentMethodType()));
        detailCreatedValueLabel.setText(payment.getPaymentDate() == null ? "-" : DETAIL_CREATED_AT_FORMAT.format(payment.getPaymentDate()));

        detailCardOneTitleLabel.setText("ID Viagem");
        detailCardOneValueLabel.setText(payment.getTripId() == null ? "-" : "#" + payment.getTripId());
        detailCardTwoTitleLabel.setText("Valor");
        detailCardTwoValueLabel.setText(formatPaymentAmount(payment));
        detailCardThreeTitleLabel.setText("Estado");
        detailCardThreeValueLabel.setText(prettyPaymentStatus(payment.getStatus()));
        detailCardFourTitleLabel.setText("Moeda");
        detailCardFourValueLabel.setText(fallback(payment.getCurrencyCode()));

        detailReferenceTitleLabel.setText("Taxa aplicada");
        detailReferenceValueLabel.setText(payment.getTaxRateApplied() == null
                ? "-"
                : (payment.getTaxRateApplied().multiply(new BigDecimal("100"))).setScale(2, RoundingMode.HALF_UP) + "%");

        detailExtraOneTitleLabel.setText("Motorista");
        detailExtraOneValueLabel.setText(fallback(payment.getDriverName()));
        detailExtraTwoTitleLabel.setText("Data pagamento");
        detailExtraTwoValueLabel.setText(payment.getPaymentDate() == null ? "-" : PAYMENT_DATE_FORMAT.format(payment.getPaymentDate()));
        detailExtraThreeTitleLabel.setText("Periodo");
        detailExtraThreeValueLabel.setText(prettyFinancialPeriod(currentFinancialPeriod));
    }

    private void updateFinancialDetailPanel() {
        AdminFinancialOverviewDTO overview = currentFinancialOverview;
        if (overview == null) {
            clearDetailPanel();
            return;
        }

        detailTitleLabel.setText("Resumo financeiro");
        detailInitialsLabel.setText("EUR");
        detailNameLabel.setText("Rendimentos e pagamentos");
        detailEmailLabel.setText("Periodo: " + prettyFinancialPeriod(currentFinancialPeriod));
        detailStatusLabel.setText("Total");
        detailRoleLabel.setText("Financeiro");
        detailPhoneValueLabel.setText("Taxas IVA ativas: " + allTaxRates.stream().filter(t -> Boolean.TRUE.equals(t.getActive())).count());
        detailCreatedValueLabel.setText("Pagamentos: " + overview.getTotalPayments());

        detailCardOneTitleLabel.setText("Rend. total");
        detailCardOneValueLabel.setText(formatCurrency(overview.getTotalIncome()));
        detailCardTwoTitleLabel.setText("Rend. periodo");
        detailCardTwoValueLabel.setText(formatCurrency(overview.getPeriodIncome()));
        detailCardThreeTitleLabel.setText("Processados");
        detailCardThreeValueLabel.setText(String.valueOf(overview.getProcessedPayments()));
        detailCardFourTitleLabel.setText("Pendentes");
        detailCardFourValueLabel.setText(String.valueOf(overview.getPendingPayments()));

        detailReferenceTitleLabel.setText("Falhados");
        detailReferenceValueLabel.setText(String.valueOf(overview.getFailedPayments()));
        detailExtraOneTitleLabel.setText("Reembolsados");
        detailExtraOneValueLabel.setText(String.valueOf(overview.getRefundedPayments()));
        detailExtraTwoTitleLabel.setText("Top IVA");
        detailExtraTwoValueLabel.setText(formatTopTaxRates());
        detailExtraThreeTitleLabel.setText("Acao");
        detailExtraThreeValueLabel.setText("Use o botao 'Editar IVA' para atualizar taxas.");
    }

    private void updateFinancialDashboard() {
        updateFinancialPeriodButtons();

        AdminFinancialOverviewDTO overview = currentFinancialOverview;
        if (overview == null) {
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

        BigDecimal periodIncome = defaultAmount(overview.getPeriodIncome());
        BigDecimal commission = periodIncome.multiply(new BigDecimal("0.20"));
        long processed = overview.getProcessedPayments();
        long total = Math.max(overview.getTotalPayments(), 0);
        long refunded = overview.getRefundedPayments();
        long failed = overview.getFailedPayments();

        BigDecimal avgTicket = processed <= 0
                ? BigDecimal.ZERO
                : periodIncome.divide(BigDecimal.valueOf(processed), 2, RoundingMode.HALF_UP);
        long paidDrivers = allPayments.stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.PROCESSED)
                .map(payment -> normalize(payment.getDriverName()))
                .filter(value -> !value.isBlank())
                .distinct()
                .count();

        double processedRate = total == 0 ? 0 : (processed * 100.0) / total;
        double failedRate = total == 0 ? 0 : (failed * 100.0) / total;

        revenuePeriodLabel.setText("Receita Total - " + prettyFinancialPeriod(currentFinancialPeriod));
        revenueTotalLabel.setText(formatCurrency(periodIncome));
        revenueTrendLabel.setText("Taxa de sucesso: " + formatPercent(processedRate) + " | Pagamentos: " + total);

        netRevenueValueLabel.setText(formatCurrency(periodIncome));
        platformCommissionValueLabel.setText(formatCurrency(commission));
        billedTripsValueLabel.setText(String.valueOf(processed));
        avgTicketValueLabel.setText(formatCurrency(avgTicket));

        processedPaymentsValueLabel.setText(String.valueOf(processed));
        refundedPaymentsValueLabel.setText(String.valueOf(refunded));
        failedRateValueLabel.setText(formatPercent(failedRate));
        paidDriversValueLabel.setText(String.valueOf(paidDrivers));

        renderDailyRevenueBars(allPayments);
        updatePaymentMethodsLegend(groupPaymentMethods(allPayments), total);
    }

    private void updateFinancialPeriodButtons() {
        setButtonState(periodDayButton, currentFinancialPeriod == AdminFinancialPeriod.DAY, "period-chip", "period-chip-active");
        setButtonState(periodWeekButton, currentFinancialPeriod == AdminFinancialPeriod.WEEK, "period-chip", "period-chip-active");
        setButtonState(periodMonthButton, currentFinancialPeriod == AdminFinancialPeriod.MONTH, "period-chip", "period-chip-active");
        setButtonState(periodYearButton, currentFinancialPeriod == AdminFinancialPeriod.YEAR, "period-chip", "period-chip-active");
        setButtonState(periodAllButton, currentFinancialPeriod == AdminFinancialPeriod.ALL, "period-chip", "period-chip-active");
    }

    private void renderDailyRevenueBars(List<AdminPaymentByTripDTO> payments) {
        dailyBarsContainer.getChildren().clear();

        List<RevenueBucket> buckets = buildRevenueBuckets(payments);
        BigDecimal maxValue = buckets.stream()
                .map(RevenueBucket::value)
                .reduce(BigDecimal.ZERO, BigDecimal::max);

        if (maxValue.compareTo(BigDecimal.ZERO) == 0) {
            dailyBarsContainer.setAlignment(Pos.CENTER_LEFT);
            Label emptyStateLabel = new Label("Sem receita processada no periodo selecionado.");
            emptyStateLabel.getStyleClass().add("financial-empty-state");
            dailyBarsContainer.getChildren().add(emptyStateLabel);
            return;
        }

        dailyBarsContainer.setAlignment(Pos.BOTTOM_LEFT);

        double barWidth = buckets.size() <= 8
            ? 26
            : buckets.size() <= 12
            ? 20
            : buckets.size() <= 20
            ? 14
            : 9;

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

            Tooltip.install(bar, new Tooltip(bucket.tooltip() + " | " + formatCurrency(bucket.value())));

            column.getChildren().addAll(valueLabel, bar, bucketLabel);
            dailyBarsContainer.getChildren().add(column);
        }
    }

    private List<RevenueBucket> buildRevenueBuckets(List<AdminPaymentByTripDTO> payments) {
        List<AdminPaymentByTripDTO> processedPayments = payments == null
                ? List.of()
                : payments.stream()
                .filter(payment -> payment.getStatus() == PaymentStatus.PROCESSED)
                .filter(payment -> payment.getPaymentDate() != null)
                .sorted(Comparator.comparing(AdminPaymentByTripDTO::getPaymentDate))
                .toList();

        return switch (currentFinancialPeriod) {
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
            String label = String.format(Locale.ROOT, "%02dh", start.getHour());
            String tooltip = start.format(PAYMENT_DATE_FORMAT) + " - " + end.format(PAYMENT_DATE_FORMAT);
            buckets.add(new RevenueBucket(label, tooltip, value));
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
            String tooltip = "Dia " + label;
            buckets.add(new RevenueBucket(label, tooltip, value));
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
            String label = String.valueOf(day.getDayOfMonth());
            String tooltip = "Dia " + day.format(DAY_LABEL_FORMAT);
            buckets.add(new RevenueBucket(label, tooltip, value));
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
        int minYear = payments.stream()
                .mapToInt(payment -> payment.getPaymentDate().getYear())
                .min()
                .orElse(currentYear - 4);
        int startYear = Math.min(minYear, currentYear - 4);

        for (int year = startYear; year <= currentYear; year++) {
            LocalDateTime start = LocalDate.of(year, 1, 1).atStartOfDay();
            LocalDateTime end = LocalDate.of(year + 1, 1, 1).atStartOfDay();
            BigDecimal value = sumPaymentsBetween(payments, start, end);
            String label = String.valueOf(year);
            String tooltip = "Ano " + year;
            buckets.add(new RevenueBucket(label, tooltip, value));
        }

        return buckets;
    }

    private BigDecimal sumPaymentsForDay(List<AdminPaymentByTripDTO> payments, LocalDate day) {
        return payments.stream()
                .filter(payment -> payment.getPaymentDate().toLocalDate().equals(day))
                .map(payment -> defaultAmount(payment.getAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private BigDecimal sumPaymentsBetween(List<AdminPaymentByTripDTO> payments, LocalDateTime start, LocalDateTime end) {
        return payments.stream()
                .filter(payment -> !payment.getPaymentDate().isBefore(start))
                .filter(payment -> payment.getPaymentDate().isBefore(end))
                .map(payment -> defaultAmount(payment.getAmount()))
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String formatCompactCurrency(BigDecimal value) {
        BigDecimal safeValue = defaultAmount(value);
        if (safeValue.compareTo(BigDecimal.ZERO) == 0) {
            return "EUR 0";
        }
        if (safeValue.compareTo(new BigDecimal("1000")) >= 0) {
            BigDecimal compact = safeValue.divide(new BigDecimal("1000"), 1, RoundingMode.HALF_UP);
            return "EUR " + compact + "k";
        }
        return "EUR " + safeValue.setScale(0, RoundingMode.HALF_UP);
    }

    private void updatePaymentMethodsLegend(Map<String, Long> groupedMethods, long totalPayments) {
        List<Map.Entry<String, Long>> sorted = groupedMethods.entrySet().stream()
                .sorted((left, right) -> Long.compare(right.getValue(), left.getValue()))
                .limit(4)
                .toList();

        setPaymentMethodLegendRow(sorted, 0, methodOneLabel, methodOnePercentLabel, totalPayments);
        setPaymentMethodLegendRow(sorted, 1, methodTwoLabel, methodTwoPercentLabel, totalPayments);
        setPaymentMethodLegendRow(sorted, 2, methodThreeLabel, methodThreePercentLabel, totalPayments);
        setPaymentMethodLegendRow(sorted, 3, methodFourLabel, methodFourPercentLabel, totalPayments);
    }

    private void setPaymentMethodLegendRow(
            List<Map.Entry<String, Long>> sorted,
            int index,
            Label methodLabel,
            Label percentLabel,
            long totalPayments) {
        if (index >= sorted.size()) {
            methodLabel.setText("-");
            percentLabel.setText("0%");
            return;
        }

        Map.Entry<String, Long> entry = sorted.get(index);
        methodLabel.setText(prettyPaymentMethod(entry.getKey()));
        double percentage = totalPayments == 0 ? 0 : (entry.getValue() * 100.0) / totalPayments;
        percentLabel.setText(formatPercent(percentage));
    }

    private Map<String, Long> groupPaymentMethods(List<AdminPaymentByTripDTO> payments) {
        Map<String, Long> grouped = new HashMap<>();
        for (AdminPaymentByTripDTO payment : payments) {
            String key = normalizePaymentMethodKey(payment.getPaymentMethodType());
            grouped.put(key, grouped.getOrDefault(key, 0L) + 1);
        }
        return grouped;
    }

    private void updateDetailsPanel(AdminUserDTO user) {
        if (user == null) {
            clearDetailPanel();
            return;
        }

        detailInitialsLabel.setText(extractInitials(user.getName()));
        detailNameLabel.setText(fallback(user.getName()));
        detailEmailLabel.setText(fallback(user.getEmail()));
        detailStatusLabel.setText(prettyStatus(user.getStatus()));
        detailRoleLabel.setText(currentSection.badgeLabel);
        detailPhoneValueLabel.setText(fallback(user.getPhone()));
        detailCreatedValueLabel
                .setText(user.getCreatedAt() == null ? "-" : DETAIL_CREATED_AT_FORMAT.format(user.getCreatedAt()));

        if (currentSection == AdminSection.DRIVERS) {
            detailCardOneTitleLabel.setText("Avaliacao");
            detailCardOneValueLabel.setText(starRating(user.getAverageRating()));
            detailCardTwoTitleLabel.setText("Viagens");
            detailCardTwoValueLabel.setText(String.valueOf(defaultInteger(user.getTotalTrips())));
            detailCardThreeTitleLabel.setText("Disponivel");
            detailCardThreeValueLabel.setText(Boolean.TRUE.equals(user.getAvailable()) ? "Online" : "Offline");
            detailCardFourTitleLabel.setText("Estado");
            detailCardFourValueLabel.setText(prettyStatus(user.getStatus()));
            detailReferenceTitleLabel.setText("N de Licenca");
            detailReferenceValueLabel.setText(fallback(user.getLicenseNumber()));
        } else {
            detailCardOneTitleLabel.setText("Estado");
            detailCardOneValueLabel.setText(prettyStatus(user.getStatus()));
            detailCardTwoTitleLabel.setText("Email");
            detailCardTwoValueLabel.setText(fallback(user.getEmail()));
            detailCardThreeTitleLabel.setText("Metodo padrao");
            detailCardThreeValueLabel
                    .setText(user.getDefaultPaymentMethodId() == null ? "-" : "#" + user.getDefaultPaymentMethodId());
            detailCardFourTitleLabel.setText("Conta");
            detailCardFourValueLabel.setText("Cliente");
            detailReferenceTitleLabel.setText("NIF");
            detailReferenceValueLabel.setText(fallback(user.getTaxNumber()));
        }
    }

    private void clearDetailPanel() {
        if (currentSection == AdminSection.FINANCIAL) {
            detailTitleLabel.setText("Resumo financeiro");
            detailInitialsLabel.setText("EUR");
            detailNameLabel.setText("Sem dados financeiros");
            detailEmailLabel.setText("-");
            detailStatusLabel.setText("-");
            detailRoleLabel.setText(currentSection.badgeLabel);
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
            detailPhoneValueLabel.setText("-");
            detailCreatedValueLabel.setText("-");
            detailExtraOneTitleLabel.setText("Reembolsados");
            detailExtraOneValueLabel.setText("-");
            detailExtraTwoTitleLabel.setText("Top IVA");
            detailExtraTwoValueLabel.setText("-");
            detailExtraThreeTitleLabel.setText("Acao");
            detailExtraThreeValueLabel.setText("-");
            return;
        }

        detailTitleLabel.setText(currentSection == AdminSection.TRIPS ? "Detalhe da viagem" : "Detalhe do utilizador");
        detailInitialsLabel.setText("--");
        detailNameLabel.setText("Sem selecao");
        detailEmailLabel.setText("-");
        detailStatusLabel.setText("-");
        detailRoleLabel.setText(currentSection.badgeLabel);
        detailCardOneTitleLabel.setText(currentSection == AdminSection.DRIVERS ? "Avaliacao" : "Estado");
        detailCardOneValueLabel.setText("-");
        detailCardTwoTitleLabel.setText(currentSection == AdminSection.DRIVERS ? "Viagens" : "Email");
        detailCardTwoValueLabel.setText("-");
        detailCardThreeTitleLabel.setText(currentSection == AdminSection.DRIVERS ? "Disponivel" : "Metodo padrao");
        detailCardThreeValueLabel.setText("-");
        detailCardFourTitleLabel.setText(currentSection == AdminSection.DRIVERS ? "Estado" : "Conta");
        detailCardFourValueLabel.setText("-");
        detailReferenceTitleLabel.setText(currentSection == AdminSection.DRIVERS ? "N de Licenca" : "NIF");
        detailReferenceValueLabel.setText("-");
        detailPhoneValueLabel.setText("-");
        detailCreatedValueLabel.setText("-");
        detailExtraOneTitleLabel.setText(currentSection == AdminSection.TRIPS ? "Preco final" : "");
        detailExtraOneValueLabel.setText("-");
        detailExtraTwoTitleLabel.setText(currentSection == AdminSection.TRIPS ? "Inicio" : "");
        detailExtraTwoValueLabel.setText("-");
        detailExtraThreeTitleLabel.setText(currentSection == AdminSection.TRIPS ? "Fim" : "");
        detailExtraThreeValueLabel.setText("-");
    }

    private String metricValue(AdminUserDTO user) {
        if (currentSection == AdminSection.DRIVERS) {
            return starRating(user.getAverageRating());
        }
        return fallback(user.getEmail());
    }

    private String volumeValue(AdminUserDTO user) {
        if (currentSection == AdminSection.DRIVERS) {
            return String.valueOf(defaultInteger(user.getTotalTrips()));
        }
        return fallback(user.getPhone());
    }

    private String referenceValue(AdminUserDTO user) {
        if (currentSection == AdminSection.DRIVERS) {
            return fallback(user.getLicenseNumber());
        }
        return fallback(user.getTaxNumber());
    }

    private void showFeedback(String message, boolean isError) {
        feedbackLabel.setText(message);
        feedbackLabel.getStyleClass().removeAll("feedback-success", "feedback-error");
        feedbackLabel.getStyleClass().add(isError ? "feedback-error" : "feedback-success");
    }

    private void openUserFormModal(AdminUserDTO editingUser) {
        boolean editing = editingUser != null;

        modalMode = editing ? ModalMode.EDIT : ModalMode.CREATE;
        modalTargetUser = editingUser;

        modalTitleLabel.setText(editing
                ? "Editar " + currentSection.badgeLabel.toLowerCase(Locale.ROOT)
                : "Novo " + currentSection.badgeLabel.toLowerCase(Locale.ROOT));

        modalUserFormSection.setVisible(true);
        modalUserFormSection.setManaged(true);
        modalTripFormSection.setVisible(false);
        modalTripFormSection.setManaged(false);
        modalTaxRateFormSection.setVisible(false);
        modalTaxRateFormSection.setManaged(false);
        modalDeleteSection.setVisible(false);
        modalDeleteSection.setManaged(false);

        modalSaveButton.setVisible(true);
        modalSaveButton.setManaged(true);
        modalDeleteConfirmButton.setVisible(false);
        modalDeleteConfirmButton.setManaged(false);

        modalReferenceLabel.setText(currentSection == AdminSection.DRIVERS ? "Licenca" : "NIF");
        modalReferenceField.setPromptText(currentSection == AdminSection.DRIVERS ? "Numero de licenca" : "NIF");

        if (editing) {
            modalNameField.setText(fallback(editingUser.getName()));
            modalEmailField.setText(fallback(editingUser.getEmail()));
            modalPhoneField.setText(fallback(editingUser.getPhone()));
            modalReferenceField.setText(fallback(currentSection == AdminSection.DRIVERS
                    ? editingUser.getLicenseNumber()
                    : editingUser.getTaxNumber()));
            modalStatusCombo.setValue(editingUser.getStatus());
        } else {
            modalNameField.clear();
            modalEmailField.clear();
            modalPhoneField.clear();
            modalReferenceField.clear();
            modalStatusCombo.setValue(AccountStatus.ACTIVE);
        }

        modalPasswordField.clear();
        modalConfirmPasswordField.clear();
        clearModalError();
        showModal();
    }

    private void openTripFormModal() {
        openTripFormModal(null);
    }

    private void openTripFormModal(AdminTripDTO editingTrip) {
        boolean editing = editingTrip != null;
        modalMode = editing ? ModalMode.EDIT_TRIP : ModalMode.CREATE_TRIP;
        modalTargetTrip = editingTrip;
        modalTargetUser = null;

        modalTitleLabel.setText(editing ? "Editar viagem" : "Nova viagem");

        modalUserFormSection.setVisible(false);
        modalUserFormSection.setManaged(false);
        modalTripFormSection.setVisible(true);
        modalTripFormSection.setManaged(true);
        modalTaxRateFormSection.setVisible(false);
        modalTaxRateFormSection.setManaged(false);
        modalDeleteSection.setVisible(false);
        modalDeleteSection.setManaged(false);

        modalSaveButton.setVisible(true);
        modalSaveButton.setManaged(true);
        modalDeleteConfirmButton.setVisible(false);
        modalDeleteConfirmButton.setManaged(false);

        if (editing) {
            modalTripClientIdField.setText(editingTrip.getClientId() == null ? "" : String.valueOf(editingTrip.getClientId()));
            modalTripDriverIdField.setText(editingTrip.getDriverId() == null ? "" : String.valueOf(editingTrip.getDriverId()));
            modalTripTypeCombo.setValue(editingTrip.getTripType() == null ? TripType.IMMEDIATE : editingTrip.getTripType());
            modalTripStatusCombo.setValue(editingTrip.getStatus() == null ? TripStatus.PENDING : editingTrip.getStatus());
            modalTripOriginField.setText(fallback(editingTrip.getOriginAddress()).equals("-") ? "" : editingTrip.getOriginAddress());
            modalTripDestinationField.setText(fallback(editingTrip.getDestinationAddress()).equals("-") ? "" : editingTrip.getDestinationAddress());
            modalTripEstimatedPriceField
                .setText(editingTrip.getEstimatedPrice() == null ? "" : editingTrip.getEstimatedPrice().toPlainString());
            modalTripFinalPriceField
                .setText(editingTrip.getFinalPrice() == null ? "" : editingTrip.getFinalPrice().toPlainString());
            modalTripNotesField.setText(fallback(editingTrip.getNotes()).equals("-") ? "" : editingTrip.getNotes());
        } else {
            modalTripClientIdField.clear();
            modalTripDriverIdField.clear();
            modalTripTypeCombo.setValue(TripType.IMMEDIATE);
            modalTripStatusCombo.setValue(TripStatus.PENDING);
            modalTripOriginField.clear();
            modalTripDestinationField.clear();
            modalTripEstimatedPriceField.clear();
            modalTripFinalPriceField.clear();
            modalTripNotesField.clear();
        }

        clearModalError();
        showModal();
    }

    private void openDeleteConfirmModal(AdminUserDTO selectedUser) {
        modalMode = ModalMode.DELETE_CONFIRM;
        modalTargetUser = selectedUser;

        modalTitleLabel
                .setText(selectedUser.getStatus() == AccountStatus.BLOCKED ? "Apagar registo" : "Bloquear conta");

        modalUserFormSection.setVisible(false);
        modalUserFormSection.setManaged(false);
        modalTripFormSection.setVisible(false);
        modalTripFormSection.setManaged(false);
        modalTaxRateFormSection.setVisible(false);
        modalTaxRateFormSection.setManaged(false);
        modalDeleteSection.setVisible(true);
        modalDeleteSection.setManaged(true);

        modalSaveButton.setVisible(false);
        modalSaveButton.setManaged(false);
        modalDeleteConfirmButton.setVisible(true);
        modalDeleteConfirmButton.setManaged(true);

        if (selectedUser.getStatus() == AccountStatus.BLOCKED) {
            modalDeleteMessageLabel.setText("Apagar " + selectedUser.getName() + "? Esta acao e permanente.");
            modalDeleteConfirmButton.setText("Apagar");
        } else {
            modalDeleteMessageLabel
                    .setText("Bloquear " + selectedUser.getName() + "? A conta sera marcada como bloqueada.");
            modalDeleteConfirmButton.setText("Bloquear");
        }

        clearModalError();
        showModal();
    }

    private void openDeleteTripConfirmModal(AdminTripDTO selectedTrip) {
        modalMode = ModalMode.DELETE_TRIP_CONFIRM;
        modalTargetTrip = selectedTrip;
        modalTargetUser = null;

        modalTitleLabel.setText("Apagar viagem");

        modalUserFormSection.setVisible(false);
        modalUserFormSection.setManaged(false);
        modalTripFormSection.setVisible(false);
        modalTripFormSection.setManaged(false);
        modalTaxRateFormSection.setVisible(false);
        modalTaxRateFormSection.setManaged(false);
        modalDeleteSection.setVisible(true);
        modalDeleteSection.setManaged(true);

        modalSaveButton.setVisible(false);
        modalSaveButton.setManaged(false);
        modalDeleteConfirmButton.setVisible(true);
        modalDeleteConfirmButton.setManaged(true);
        modalDeleteConfirmButton.setText("Apagar");

        modalDeleteMessageLabel.setText("Tem a certeza que deseja apagar a viagem #" + selectedTrip.getId()
            + "?\nRota: " + fallback(selectedTrip.getOriginAddress())
            + " -> " + fallback(selectedTrip.getDestinationAddress())
            + ".");

        clearModalError();
        showModal();
    }

    private void persistCreateUser() {
        try {
            AdminUserCommand command = new AdminUserCommand(
                    modalNameField.getText(),
                    modalEmailField.getText(),
                    modalPhoneField.getText(),
                    modalReferenceField.getText(),
                    modalStatusCombo.getValue(),
                    modalPasswordField.getText(),
                    currentSection.userType);

            adminService.createUser(command);
            hideModal();
            refreshSectionData();
            showFeedback(currentSection.badgeLabel + " criado com sucesso.", false);
        } catch (Exception exception) {
            showModalError("Falha ao criar registo: " + exception.getMessage());
        }
    }

    private void openTaxRateEditModal() {
        if (allTaxRates.isEmpty()) {
            showFeedback("Nao existem taxas de IVA para editar.", true);
            return;
        }

        modalMode = ModalMode.EDIT_TAX_RATE;
        modalTitleLabel.setText("Editar taxa de IVA");

        modalUserFormSection.setVisible(false);
        modalUserFormSection.setManaged(false);
        modalTripFormSection.setVisible(false);
        modalTripFormSection.setManaged(false);
        modalTaxRateFormSection.setVisible(true);
        modalTaxRateFormSection.setManaged(true);
        modalDeleteSection.setVisible(false);
        modalDeleteSection.setManaged(false);

        modalSaveButton.setVisible(true);
        modalSaveButton.setManaged(true);
        modalDeleteConfirmButton.setVisible(false);
        modalDeleteConfirmButton.setManaged(false);

        modalTaxRateCombo.setItems(FXCollections.observableArrayList(allTaxRates));
        modalTaxRateCombo.getSelectionModel().selectFirst();
        populateTaxRateFields(modalTaxRateCombo.getValue());

        clearModalError();
        showModal();
    }

    private void populateTaxRateFields(AdminTaxRateDTO taxRate) {
        if (taxRate == null) {
            modalTaxRateNameField.clear();
            modalTaxRateValueField.clear();
            modalTaxRateDescriptionField.clear();
            modalTaxRateActiveCheck.setSelected(false);
            return;
        }

        modalTaxRateNameField.setText(fallback(taxRate.getName()).equals("-") ? "" : taxRate.getName());
        modalTaxRateValueField.setText(taxRate.getRate() == null ? "" : taxRate.getRate().toPlainString());
        modalTaxRateDescriptionField
                .setText(fallback(taxRate.getDescription()).equals("-") ? "" : taxRate.getDescription());
        modalTaxRateActiveCheck.setSelected(Boolean.TRUE.equals(taxRate.getActive()));
    }

    private void persistTaxRateUpdate() {
        try {
            AdminTaxRateDTO selectedTaxRate = modalTaxRateCombo.getValue();
            if (selectedTaxRate == null || selectedTaxRate.getId() == null) {
                showModalError("Selecione uma taxa de IVA valida.");
                return;
            }

            BigDecimal rate = parseRequiredDecimal(modalTaxRateValueField.getText(), "Taxa de IVA");
            AdminTaxRateCommand command = new AdminTaxRateCommand(
                    modalTaxRateNameField.getText(),
                    rate,
                    modalTaxRateDescriptionField.getText(),
                    modalTaxRateActiveCheck.isSelected());

            adminService.updateTaxRate(selectedTaxRate.getId(), command);
            hideModal();
            refreshSectionData();
            showFeedback("Taxa de IVA atualizada com sucesso.", false);
        } catch (Exception exception) {
            showModalError("Falha ao atualizar taxa de IVA: " + exception.getMessage());
        }
    }

    private void persistTrip() {
        try {
            AdminTripCommand command = new AdminTripCommand(
                    parseRequiredInteger(modalTripClientIdField.getText(), "Cliente ID"),
                    parseOptionalInteger(modalTripDriverIdField.getText()),
                    modalTripOriginField.getText(),
                    modalTripDestinationField.getText(),
                    modalTripTypeCombo.getValue(),
                    modalTripStatusCombo.getValue(),
                    modalTripNotesField.getText(),
                    parseOptionalDecimal(modalTripEstimatedPriceField.getText()),
                    parseOptionalDecimal(modalTripFinalPriceField.getText()));

            if (modalMode == ModalMode.EDIT_TRIP) {
                if (modalTargetTrip == null || modalTargetTrip.getId() == null) {
                    showModalError("Viagem invalida.");
                    return;
                }
                adminService.updateTrip(modalTargetTrip.getId(), command);
                showFeedback("Viagem atualizada com sucesso.", false);
            } else {
                adminService.createTrip(command);
                showFeedback("Viagem criada com sucesso.", false);
            }
            hideModal();
            refreshSectionData();
        } catch (Exception exception) {
            showModalError("Falha ao guardar viagem: " + exception.getMessage());
        }
    }

    private void persistEditUser() {
        try {
            if (modalTargetUser == null) {
                showModalError("Registo invalido.");
                return;
            }

            AdminUserCommand command = new AdminUserCommand(
                    modalNameField.getText(),
                    modalEmailField.getText(),
                    modalPhoneField.getText(),
                    modalReferenceField.getText(),
                    modalStatusCombo.getValue(),
                    modalPasswordField.getText(),
                    currentSection.userType);

            adminService.updateUser(modalTargetUser.getId(), command);
            hideModal();
            refreshSectionData();
            showFeedback(currentSection.badgeLabel + " atualizado com sucesso.", false);
        } catch (Exception exception) {
            showModalError("Falha ao atualizar registo: " + exception.getMessage());
        }
    }

    private void showModal() {
        modalOverlay.setVisible(true);
        modalOverlay.setManaged(true);
    }

    private void hideModal() {
        modalMode = ModalMode.NONE;
        modalTargetUser = null;
        modalTargetTrip = null;
        modalUserFormSection.setVisible(false);
        modalUserFormSection.setManaged(false);
        modalTripFormSection.setVisible(false);
        modalTripFormSection.setManaged(false);
        modalTaxRateFormSection.setVisible(false);
        modalTaxRateFormSection.setManaged(false);
        modalDeleteSection.setVisible(false);
        modalDeleteSection.setManaged(false);
        modalOverlay.setVisible(false);
        modalOverlay.setManaged(false);
        clearModalError();
    }

    private void clearModalError() {
        modalErrorLabel.setVisible(false);
        modalErrorLabel.setManaged(false);
        modalErrorLabel.setText("");
    }

    private void showModalError(String message) {
        modalErrorLabel.setText(message);
        modalErrorLabel.setVisible(true);
        modalErrorLabel.setManaged(true);
    }

    private String prettyStatus(AccountStatus status) {
        if (status == null) {
            return "-";
        }

        return switch (status) {
            case ACTIVE -> "Online";
            case INACTIVE -> "Offline";
            case BLOCKED -> "Bloqueado";
            case PENDING -> "Pendente";
        };
    }

    private String prettyUserType(UserType type) {
        if (type == null) {
            return "-";
        }

        return switch (type) {
            case ADMIN -> "Administrador";
            case DRIVER -> "Motorista";
            case CLIENT -> "Cliente";
        };
    }

    private String prettyTripStatus(com.obar.model.enums.TripStatus status) {
        if (status == null) {
            return "-";
        }

        return switch (status) {
            case PENDING -> "Pendente";
            case ACCEPTED -> "Aceite";
            case IN_PROGRESS -> "Em progresso";
            case COMPLETED -> "Concluida";
            case CANCELLED -> "Cancelada";
            case REJECTED -> "Rejeitada";
        };
    }

    private String prettyTripType(com.obar.model.enums.TripType tripType) {
        if (tripType == null) {
            return "-";
        }

        return switch (tripType) {
            case IMMEDIATE -> "Imediata";
            case SCHEDULED -> "Agendada";
        };
    }

    private String prettyPaymentStatus(PaymentStatus status) {
        if (status == null) {
            return "-";
        }

        return switch (status) {
            case PENDING -> "Pendente";
            case PROCESSED -> "Processado";
            case FAILED -> "Falhado";
            case REFUNDED -> "Reembolsado";
        };
    }

    private String prettyPaymentMethod(String rawType) {
        String normalized = normalize(rawType);
        if (normalized.isBlank() || normalized.equals("-")) {
            return "Outros";
        }
        if (normalized.contains("mb")) {
            return "MB Way";
        }
        if (normalized.contains("paypal")) {
            return "PayPal";
        }
        if (normalized.contains("visa")) {
            return "Visa";
        }
        if (normalized.contains("master")) {
            return "Mastercard";
        }
        if (normalized.contains("cartao") || normalized.contains("card") || normalized.contains("credito")) {
            return "Cartao";
        }
        return rawType.trim();
    }

    private String normalizePaymentMethodKey(String rawType) {
        return normalize(prettyPaymentMethod(rawType));
    }

    private String prettyFinancialPeriod(AdminFinancialPeriod period) {
        if (period == null) {
            return "-";
        }
        return switch (period) {
            case DAY -> "Ultimo dia";
            case WEEK -> "Ultima semana";
            case MONTH -> "Ultimo mes";
            case YEAR -> "Ultimo ano";
            case ALL -> "Todo o historico";
        };
    }

    private String formatTripPrice(AdminTripDTO trip) {
        BigDecimal amount = trip.getFinalPrice() != null ? trip.getFinalPrice() : trip.getEstimatedPrice();
        if (amount == null) {
            return "-";
        }
        return "EUR " + amount;
    }

    private String formatPaymentAmount(AdminPaymentByTripDTO payment) {
        if (payment.getAmount() == null) {
            return "-";
        }
        return payment.getCurrencyCode() + " " + payment.getAmount();
    }

    private String formatCurrency(BigDecimal value) {
        if (value == null) {
            return "EUR 0.00";
        }
        return "EUR " + value;
    }

    private String formatPercent(double value) {
        return String.format(Locale.US, "%.1f%%", value);
    }

    private BigDecimal defaultAmount(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private String formatTopTaxRates() {
        if (allTaxRates.isEmpty()) {
            return "-";
        }

        return allTaxRates.stream()
                .filter(rate -> Boolean.TRUE.equals(rate.getActive()))
                .limit(3)
                .map(this::formatTaxRateDisplay)
                .reduce((left, right) -> left + " | " + right)
                .orElse("-");
    }

    private String formatTaxRateDisplay(AdminTaxRateDTO taxRate) {
        if (taxRate == null) {
            return "-";
        }
        return fallback(taxRate.getName()) + " (" + taxRate.getRate() + ")";
    }

    private String starRating(Float rating) {
        if (rating == null) {
            return "-";
        }
        return String.format(Locale.US, "%.1f", rating);
    }

    private int defaultInteger(Integer value) {
        return value == null ? 0 : value;
    }

    private String extractInitials(String name) {
        String safeName = fallback(name);
        if (safeName.equals("-")) {
            return "--";
        }

        String[] parts = safeName.trim().split("\\s+");
        if (parts.length == 1) {
            return parts[0].substring(0, Math.min(2, parts[0].length())).toUpperCase(Locale.ROOT);
        }

        String first = parts[0].substring(0, 1);
        String last = parts[parts.length - 1].substring(0, 1);
        return (first + last).toUpperCase(Locale.ROOT);
    }

    private String fallback(String value) {
        if (value == null || value.isBlank()) {
            return "-";
        }
        return value;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
    }

    private Integer parseRequiredInteger(String rawValue, String fieldName) {
        Integer parsed = parseOptionalInteger(rawValue);
        if (parsed == null) {
            throw new IllegalArgumentException(fieldName + " e obrigatorio.");
        }
        return parsed;
    }

    private Integer parseOptionalInteger(String rawValue) {
        String safe = rawValue == null ? "" : rawValue.trim();
        if (safe.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(safe);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("ID invalido: " + safe);
        }
    }

    private BigDecimal parseRequiredDecimal(String rawValue, String fieldName) {
        BigDecimal parsed = parseOptionalDecimal(rawValue);
        if (parsed == null) {
            throw new IllegalArgumentException(fieldName + " e obrigatorio.");
        }
        return parsed;
    }

    private BigDecimal parseOptionalDecimal(String rawValue) {
        String safe = rawValue == null ? "" : rawValue.trim();
        if (safe.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(safe);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Valor monetario invalido: " + safe);
        }
    }

}
