package com.obar.desktop.admin;

import com.obar.bll.admin.AdminService;
import com.obar.bll.admin.AdminTripCommand;
import com.obar.bll.admin.AdminTripDTO;
import com.obar.bll.admin.AdminUserCommand;
import com.obar.bll.admin.AdminUserDTO;
import com.obar.bll.auth.AuthenticatedUserDto;
import com.obar.desktop.navigation.NavigationManager;
import com.obar.desktop.session.SessionManager;
import com.obar.model.enums.AccountStatus;
import com.obar.model.enums.TripStatus;
import com.obar.model.enums.TripType;
import com.obar.model.enums.UserType;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

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

    private enum ModalMode {
        NONE,
        CREATE,
        EDIT,
        CREATE_TRIP,
        DELETE_CONFIRM
    }

    private enum AdminSection {
        DRIVERS("Motoristas", "+ Novo Motorista", UserType.DRIVER, "Motorista"),
        CLIENTS("Clientes", "+ Novo Cliente", UserType.CLIENT, "Cliente"),
        TRIPS("Viagens", "+ Nova Viagem", null, "Viagem");

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

    private AdminSection currentSection = AdminSection.DRIVERS;
    private AccountStatus currentStatusFilter;
    private ModalMode modalMode = ModalMode.NONE;
    private AdminUserDTO modalTargetUser;

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

        usersTable.setItems(filteredUsers);
        tripsTable.setItems(filteredTrips);
        usersTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, previous, current) -> onSelectionChanged(current));
        tripsTable.getSelectionModel().selectedItemProperty()
            .addListener((obs, previous, current) -> onTripSelectionChanged(current));
        searchField.textProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        modalStatusCombo.setItems(FXCollections.observableArrayList(AccountStatus.values()));
        modalTripTypeCombo.setItems(FXCollections.observableArrayList(TripType.values()));
        modalTripStatusCombo.setItems(FXCollections.observableArrayList(TripStatus.values()));

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
    public void handleFilterAll() {
        setStatusFilter(null);
    }

    @FXML
    public void handleFilterActive() {
        setStatusFilter(AccountStatus.ACTIVE);
    }

    @FXML
    public void handleFilterInactive() {
        setStatusFilter(AccountStatus.INACTIVE);
    }

    @FXML
    public void handleFilterBlocked() {
        setStatusFilter(AccountStatus.BLOCKED);
    }

    @FXML
    public void handleFilterPending() {
        setStatusFilter(AccountStatus.PENDING);
    }

    @FXML
    public void handleExportSection() {
        showFeedback("Exportacao ainda nao implementada. Registos filtrados: " + filteredUsers.size(), false);
    }

    @FXML
    public void handleAddUser() {
        if (currentSection == AdminSection.TRIPS) {
            openTripFormModal();
            return;
        }
        openUserFormModal(null);
    }

    @FXML
    public void handleEditUser() {
        if (currentSection == AdminSection.TRIPS) {
            showFeedback("Edicao de viagens nao esta disponivel nesta vista.", true);
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
        if (currentSection == AdminSection.TRIPS) {
            showFeedback("Operacao indisponivel para viagens nesta vista.", true);
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
        if (modalMode != ModalMode.CREATE && modalMode != ModalMode.EDIT && modalMode != ModalMode.CREATE_TRIP) {
            hideModal();
            return;
        }

        if (modalMode == ModalMode.CREATE_TRIP) {
            persistCreateTrip();
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

    private void switchSection(AdminSection section) {
        this.currentSection = section;
        currentSectionLabel.setText(section.title);
        addUserButton.setText(section.createLabel);

        boolean isTripsSection = section == AdminSection.TRIPS;
        usersTable.setVisible(!isTripsSection);
        usersTable.setManaged(!isTripsSection);
        tripsTable.setVisible(isTripsSection);
        tripsTable.setManaged(isTripsSection);
        addUserButton.setVisible(true);
        addUserButton.setManaged(true);
        editUserButton.setVisible(!isTripsSection);
        editUserButton.setManaged(!isTripsSection);
        deleteUserButton.setVisible(!isTripsSection);
        deleteUserButton.setManaged(!isTripsSection);
        filterAllButton.setVisible(!isTripsSection);
        filterAllButton.setManaged(!isTripsSection);
        filterActiveButton.setVisible(!isTripsSection);
        filterActiveButton.setManaged(!isTripsSection);
        filterInactiveButton.setVisible(!isTripsSection);
        filterInactiveButton.setManaged(!isTripsSection);
        filterBlockedButton.setVisible(!isTripsSection);
        filterBlockedButton.setManaged(!isTripsSection);
        filterPendingButton.setVisible(!isTripsSection);
        filterPendingButton.setManaged(!isTripsSection);
        boolean hasUserSelection = usersTable.getSelectionModel().getSelectedItem() != null;
        boolean hasTripSelection = tripsTable.getSelectionModel().getSelectedItem() != null;
        detailPanel.setVisible(isTripsSection ? hasTripSelection : hasUserSelection);
        detailPanel.setManaged(isTripsSection ? hasTripSelection : hasUserSelection);

        if (section == AdminSection.DRIVERS) {
            nameColumn.setText("Motorista");
            emailColumn.setText("Email");
            emailColumn.setVisible(true);
            metricColumn.setText("Avaliacao");
            volumeColumn.setText("Viagens");
            referenceColumn.setText("Licenca");
            searchField.setPromptText("Pesquisar por nome, email, telefone, licenca...");
        } else if (section == AdminSection.CLIENTS) {
            nameColumn.setText("Cliente");
            emailColumn.setVisible(false);
            metricColumn.setText("Email");
            volumeColumn.setText("Telefone");
            referenceColumn.setText("NIF");
            searchField.setPromptText("Pesquisar por nome, email, telefone, NIF...");
        } else {
            searchField.setPromptText("Pesquisar por ID, cliente, motorista, estado...");
        }

        updateSectionButtonState();
        setStatusFilter(null);
        refreshSectionData();
        hideModal();
    }

    private void refreshSectionData() {
        if (currentSection == AdminSection.TRIPS) {
            allTrips.setAll(adminService.listTrips());
        } else {
            allUsers.setAll(adminService.listUsersByType(currentSection.userType));
        }
        applyFilters();
        updateFilterLabels();
        usersTable.getSelectionModel().clearSelection();
        clearDetailPanel();
    }

    private void onSelectionChanged(AdminUserDTO selectedUser) {
        if (currentSection == AdminSection.TRIPS) {
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

    private void setStatusFilter(AccountStatus status) {
        this.currentStatusFilter = status;
        updateFilterButtonState();
        applyFilters();
    }

    private void applyFilters() {
        String query = normalize(searchField.getText());
        if (currentSection == AdminSection.TRIPS) {
            filteredTrips.setPredicate(trip -> matchesTripQuery(trip, query));
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

        listInfoLabel.setText("A mostrar " + filteredUsers.size() + " de " + allUsers.size() + " "
                + currentSection.title.toLowerCase(Locale.ROOT));
    }

    private boolean matchesStatus(AdminUserDTO user) {
        return currentStatusFilter == null || user.getStatus() == currentStatusFilter;
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

    private void updateSectionButtonState() {
        setButtonState(motoristasSectionButton, currentSection == AdminSection.DRIVERS, "sidebar-item",
                "sidebar-item-active");
        setButtonState(clientesSectionButton, currentSection == AdminSection.CLIENTS, "sidebar-item",
                "sidebar-item-active");
        setButtonState(viagensSectionButton, currentSection == AdminSection.TRIPS, "sidebar-item",
                "sidebar-item-active");
    }

    private void updateFilterButtonState() {
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
        if (currentSection == AdminSection.TRIPS) {
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
        modalMode = ModalMode.CREATE_TRIP;
        modalTargetUser = null;

        modalTitleLabel.setText("Nova viagem");

        modalUserFormSection.setVisible(false);
        modalUserFormSection.setManaged(false);
        modalTripFormSection.setVisible(true);
        modalTripFormSection.setManaged(true);
        modalDeleteSection.setVisible(false);
        modalDeleteSection.setManaged(false);

        modalSaveButton.setVisible(true);
        modalSaveButton.setManaged(true);
        modalDeleteConfirmButton.setVisible(false);
        modalDeleteConfirmButton.setManaged(false);

        modalTripClientIdField.clear();
        modalTripDriverIdField.clear();
        modalTripTypeCombo.setValue(TripType.IMMEDIATE);
        modalTripStatusCombo.setValue(TripStatus.PENDING);
        modalTripOriginField.clear();
        modalTripDestinationField.clear();
        modalTripEstimatedPriceField.clear();
        modalTripFinalPriceField.clear();
        modalTripNotesField.clear();

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

    private void persistCreateTrip() {
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

            adminService.createTrip(command);
            hideModal();
            refreshSectionData();
            showFeedback("Viagem criada com sucesso.", false);
        } catch (Exception exception) {
            showModalError("Falha ao criar viagem: " + exception.getMessage());
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
        modalUserFormSection.setVisible(false);
        modalUserFormSection.setManaged(false);
        modalTripFormSection.setVisible(false);
        modalTripFormSection.setManaged(false);
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

    private String formatTripPrice(AdminTripDTO trip) {
        java.math.BigDecimal amount = trip.getFinalPrice() != null ? trip.getFinalPrice() : trip.getEstimatedPrice();
        if (amount == null) {
            return "-";
        }
        return "EUR " + amount;
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

    private java.math.BigDecimal parseOptionalDecimal(String rawValue) {
        String safe = rawValue == null ? "" : rawValue.trim();
        if (safe.isBlank()) {
            return null;
        }
        try {
            return new java.math.BigDecimal(safe);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("Valor monetario invalido: " + safe);
        }
    }

}
