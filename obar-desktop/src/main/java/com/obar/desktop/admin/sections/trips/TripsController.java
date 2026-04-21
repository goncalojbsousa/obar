package com.obar.desktop.admin.sections.trips;

import com.obar.bll.admin.AdminService;
import com.obar.bll.admin.AdminTripCommand;
import com.obar.bll.admin.AdminTripDTO;
import com.obar.desktop.admin.sections.AdminSectionController;
import com.obar.desktop.admin.shared.AdminFormatUtils;
import com.obar.model.enums.TripStatus;
import com.obar.model.enums.TripType;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public class TripsController implements AdminSectionController {

    private enum ModalMode {
        NONE,
        CREATE,
        EDIT,
        DELETE_CONFIRM
    }

    private final ObservableList<AdminTripDTO> allTrips = FXCollections.observableArrayList();
    private final FilteredList<AdminTripDTO> filteredTrips = new FilteredList<>(allTrips, trip -> true);

    private AdminService adminService;
    private TripStatus currentStatusFilter;
    private ModalMode modalMode = ModalMode.NONE;
    private AdminTripDTO modalTargetTrip;

    @FXML private VBox root;
    @FXML private TextField searchField;
    @FXML private TableView<AdminTripDTO> tripsTable;
    @FXML private TableColumn<AdminTripDTO, String> tripIdColumn;
    @FXML private TableColumn<AdminTripDTO, String> tripClientColumn;
    @FXML private TableColumn<AdminTripDTO, String> tripDriverColumn;
    @FXML private TableColumn<AdminTripDTO, String> tripStatusColumn;
    @FXML private TableColumn<AdminTripDTO, String> tripTypeColumn;
    @FXML private TableColumn<AdminTripDTO, String> tripPriceColumn;
    @FXML private TableColumn<AdminTripDTO, String> tripRequestedAtColumn;
    @FXML private Label listInfoLabel;
    @FXML private Label feedbackLabel;
    @FXML private VBox detailPanel;
    @FXML private Button addUserButton;
    @FXML private Button editUserButton;
    @FXML private Button deleteUserButton;
    @FXML private Button filterAllButton;
    @FXML private Button filterActiveButton;
    @FXML private Button filterInactiveButton;
    @FXML private Button filterBlockedButton;
    @FXML private Button filterPendingButton;

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

    @FXML private StackPane modalOverlay;
    @FXML private Label modalTitleLabel;
    @FXML private Label modalErrorLabel;
    @FXML private VBox modalDeleteSection;
    @FXML private Label modalDeleteMessageLabel;
    @FXML private TextField modalTripClientIdField;
    @FXML private TextField modalTripDriverIdField;
    @FXML private ComboBox<TripType> modalTripTypeCombo;
    @FXML private ComboBox<TripStatus> modalTripStatusCombo;
    @FXML private TextField modalTripOriginField;
    @FXML private TextField modalTripDestinationField;
    @FXML private TextField modalTripEstimatedPriceField;
    @FXML private TextField modalTripFinalPriceField;
    @FXML private TextField modalTripNotesField;
    @FXML private Button modalSaveButton;
    @FXML private Button modalDeleteConfirmButton;

    @Override
    public void setAdminService(AdminService adminService) {
        this.adminService = adminService;
    }

    @FXML
    public void initialize() {
        tripsTable.setItems(filteredTrips);
        modalTripTypeCombo.setItems(FXCollections.observableArrayList(TripType.values()));
        modalTripStatusCombo.setItems(FXCollections.observableArrayList(TripStatus.values()));
        searchField.textProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        tripsTable.getSelectionModel().selectedItemProperty().addListener((obs, previous, current) -> updateDetailsPanel(current));
        setupColumns();
        setDetailsVisible(false);
        refresh();
    }

    @Override
    public void onSectionActivated() {
        refresh();
    }

    @FXML
    public void handleFilterAll() {
        setStatusFilter(null);
    }

    @FXML
    public void handleFilterActive() {
        setStatusFilter(TripStatus.ACCEPTED);
    }

    @FXML
    public void handleFilterInactive() {
        setStatusFilter(TripStatus.IN_PROGRESS);
    }

    @FXML
    public void handleFilterBlocked() {
        setStatusFilter(TripStatus.COMPLETED);
    }

    @FXML
    public void handleFilterPending() {
        setStatusFilter(TripStatus.PENDING);
    }

    @FXML
    public void handleAddUser() {
        openTripFormModal(null);
    }

    @FXML
    public void handleEditUser() {
        AdminTripDTO selectedTrip = tripsTable.getSelectionModel().getSelectedItem();
        if (selectedTrip == null) {
            showFeedback("Selecione uma viagem primeiro.", true);
            return;
        }
        openTripFormModal(selectedTrip);
    }

    @FXML
    public void handleDeleteUser() {
        AdminTripDTO selectedTrip = tripsTable.getSelectionModel().getSelectedItem();
        if (selectedTrip == null) {
            showFeedback("Selecione uma viagem primeiro.", true);
            return;
        }
        openDeleteTripConfirmModal(selectedTrip);
    }

    @FXML
    public void handleModalCancel() {
        hideModal();
    }

    @FXML
    public void handleModalSave() {
        if (modalMode == ModalMode.DELETE_CONFIRM) {
            return;
        }
        persistTrip();
    }

    @FXML
    public void handleModalConfirmDelete() {
        if (modalMode != ModalMode.DELETE_CONFIRM || modalTargetTrip == null) {
            hideModal();
            return;
        }

        try {
            if (modalTargetTrip.getId() == null) {
                showModalError("Viagem invalida.");
                return;
            }
            adminService.deleteTrip(modalTargetTrip.getId());
            hideModal();
            refresh();
            showFeedback("Viagem apagada com sucesso.", false);
        } catch (Exception exception) {
            showModalError("Falha ao apagar viagem: " + exception.getMessage());
        }
    }

    private void setStatusFilter(TripStatus status) {
        currentStatusFilter = status;
        updateFilterChipState();
        applyFilters();
    }

    private void refresh() {
        if (adminService == null) {
            return;
        }
        allTrips.setAll(adminService.listTrips());
        applyFilters();
        updateFilterLabels();
    }

    private void setupColumns() {
        tripIdColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getId() == null ? "-" : "#" + cellData.getValue().getId()));
        tripClientColumn.setCellValueFactory(cellData -> new SimpleStringProperty(AdminFormatUtils.fallback(cellData.getValue().getClientName())));
        tripDriverColumn.setCellValueFactory(cellData -> new SimpleStringProperty(AdminFormatUtils.fallback(cellData.getValue().getDriverName())));
        tripStatusColumn.setCellValueFactory(cellData -> new SimpleStringProperty(AdminFormatUtils.prettyTripStatus(cellData.getValue().getStatus())));
        tripTypeColumn.setCellValueFactory(cellData -> new SimpleStringProperty(AdminFormatUtils.prettyTripType(cellData.getValue().getTripType())));
        tripPriceColumn.setCellValueFactory(cellData -> new SimpleStringProperty(AdminFormatUtils.formatTripPrice(cellData.getValue())));
        tripRequestedAtColumn.setCellValueFactory(cellData -> {
            LocalDateTime requestTime = cellData.getValue().getRequestTime();
            return new SimpleStringProperty(requestTime == null ? "-" : requestTime.toString());
        });
        tripStatusColumn.setCellFactory(column -> new TableCell<>() {
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
        filteredTrips.setPredicate(trip -> matchesStatus(trip) && matchesTripQuery(trip, query));
        updateFilterLabels();
    }

    private boolean matchesStatus(AdminTripDTO trip) {
        return currentStatusFilter == null || trip.getStatus() == currentStatusFilter;
    }

    private boolean matchesTripQuery(AdminTripDTO trip, String query) {
        if (query.isBlank()) {
            return true;
        }

        String idValue = trip.getId() == null ? "" : String.valueOf(trip.getId());
        return AdminFormatUtils.normalize(idValue).contains(query)
                || AdminFormatUtils.normalize(trip.getClientName()).contains(query)
                || AdminFormatUtils.normalize(trip.getDriverName()).contains(query)
                || AdminFormatUtils.normalize(AdminFormatUtils.prettyTripStatus(trip.getStatus())).contains(query)
                || AdminFormatUtils.normalize(AdminFormatUtils.prettyTripType(trip.getTripType())).contains(query)
                || AdminFormatUtils.normalize(trip.getOriginAddress()).contains(query)
                || AdminFormatUtils.normalize(trip.getDestinationAddress()).contains(query);
    }

    private void updateDetailsPanel(AdminTripDTO trip) {
        if (trip == null) {
            clearDetails();
            setDetailsVisible(false);
            return;
        }

        setDetailsVisible(true);

        detailInitialsLabel.setText(trip.getId() == null ? "--" : "#" + trip.getId());
        detailTitleLabel.setText("Detalhe da viagem");
        detailNameLabel.setText(AdminFormatUtils.fallback(trip.getOriginAddress()) + " -> " + AdminFormatUtils.fallback(trip.getDestinationAddress()));
        detailEmailLabel.setText("Cliente: " + AdminFormatUtils.fallback(trip.getClientName()));
        detailStatusLabel.setText(AdminFormatUtils.prettyTripStatus(trip.getStatus()));
        detailRoleLabel.setText(AdminFormatUtils.prettyTripType(trip.getTripType()));
        detailPhoneValueLabel.setText("Motorista: " + AdminFormatUtils.fallback(trip.getDriverName()));
        detailCreatedValueLabel.setText(trip.getRequestTime() == null ? "-" : trip.getRequestTime().toString());

        detailCardOneTitleLabel.setText("ID Cliente");
        detailCardOneValueLabel.setText(trip.getClientId() == null ? "-" : "#" + trip.getClientId());
        detailCardTwoTitleLabel.setText("ID Motorista");
        detailCardTwoValueLabel.setText(trip.getDriverId() == null ? "-" : "#" + trip.getDriverId());
        detailCardThreeTitleLabel.setText("Veiculo");
        detailCardThreeValueLabel.setText(AdminFormatUtils.fallback(trip.getVehicleDisplay()));
        detailCardFourTitleLabel.setText("Distancia");
        detailCardFourValueLabel.setText(trip.getDistanceKm() == null ? "-" : trip.getDistanceKm() + " km");
        detailReferenceTitleLabel.setText("Preco estimado");
        detailReferenceValueLabel.setText(trip.getEstimatedPrice() == null ? "-" : "EUR " + trip.getEstimatedPrice());
        detailExtraOneTitleLabel.setText("Preco final");
        detailExtraOneValueLabel.setText(trip.getFinalPrice() == null ? "-" : "EUR " + trip.getFinalPrice());
        detailExtraTwoTitleLabel.setText("Inicio");
        detailExtraTwoValueLabel.setText(trip.getStartTime() == null ? "-" : trip.getStartTime().toString());
        detailExtraThreeTitleLabel.setText("Fim");
        detailExtraThreeValueLabel.setText(trip.getEndTime() == null ? "-" : trip.getEndTime().toString());
    }

    private void clearDetails() {
        detailInitialsLabel.setText("--");
        detailTitleLabel.setText("Sem selecao");
        detailNameLabel.setText("Sem selecao");
        detailEmailLabel.setText("-");
        detailStatusLabel.setText("-");
        detailRoleLabel.setText("-");
        detailPhoneValueLabel.setText("-");
        detailCreatedValueLabel.setText("-");
        detailCardOneTitleLabel.setText("ID Cliente");
        detailCardOneValueLabel.setText("-");
        detailCardTwoTitleLabel.setText("ID Motorista");
        detailCardTwoValueLabel.setText("-");
        detailCardThreeTitleLabel.setText("Veiculo");
        detailCardThreeValueLabel.setText("-");
        detailCardFourTitleLabel.setText("Distancia");
        detailCardFourValueLabel.setText("-");
        detailReferenceTitleLabel.setText("Preco estimado");
        detailReferenceValueLabel.setText("-");
        detailExtraOneTitleLabel.setText("Preco final");
        detailExtraOneValueLabel.setText("-");
        detailExtraTwoTitleLabel.setText("Inicio");
        detailExtraTwoValueLabel.setText("-");
        detailExtraThreeTitleLabel.setText("Fim");
        detailExtraThreeValueLabel.setText("-");
    }

    @FXML
    public void handleCloseDetailPanel() {
        tripsTable.getSelectionModel().clearSelection();
        clearDetails();
        setDetailsVisible(false);
    }

    private void setDetailsVisible(boolean visible) {
        if (detailPanel == null) {
            return;
        }
        detailPanel.setVisible(visible);
        detailPanel.setManaged(visible);
    }

    private void openTripFormModal(AdminTripDTO editingTrip) {
        boolean editing = editingTrip != null;
        modalMode = editing ? ModalMode.EDIT : ModalMode.CREATE;
        modalTargetTrip = editingTrip;
        modalTitleLabel.setText(editing ? "Editar viagem" : "Nova viagem");
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
            modalTripOriginField.setText(AdminFormatUtils.fallback(editingTrip.getOriginAddress()).equals("-") ? "" : editingTrip.getOriginAddress());
            modalTripDestinationField.setText(AdminFormatUtils.fallback(editingTrip.getDestinationAddress()).equals("-") ? "" : editingTrip.getDestinationAddress());
            modalTripEstimatedPriceField.setText(editingTrip.getEstimatedPrice() == null ? "" : editingTrip.getEstimatedPrice().toPlainString());
            modalTripFinalPriceField.setText(editingTrip.getFinalPrice() == null ? "" : editingTrip.getFinalPrice().toPlainString());
            modalTripNotesField.setText(AdminFormatUtils.fallback(editingTrip.getNotes()).equals("-") ? "" : editingTrip.getNotes());
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

    private void openDeleteTripConfirmModal(AdminTripDTO selectedTrip) {
        modalMode = ModalMode.DELETE_CONFIRM;
        modalTargetTrip = selectedTrip;
        modalTitleLabel.setText("Apagar viagem");
        modalDeleteSection.setVisible(true);
        modalDeleteSection.setManaged(true);
        modalSaveButton.setVisible(false);
        modalSaveButton.setManaged(false);
        modalDeleteConfirmButton.setVisible(true);
        modalDeleteConfirmButton.setManaged(true);
        modalDeleteMessageLabel.setText("Tem a certeza que deseja apagar a viagem #" + selectedTrip.getId()
            + "?\nRota: " + AdminFormatUtils.fallback(selectedTrip.getOriginAddress())
            + " -> " + AdminFormatUtils.fallback(selectedTrip.getDestinationAddress()) + ".");
        clearModalError();
        showModal();
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

            if (modalMode == ModalMode.EDIT) {
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
            refresh();
        } catch (Exception exception) {
            showModalError("Falha ao guardar viagem: " + exception.getMessage());
        }
    }

    private void showModal() {
        modalOverlay.setVisible(true);
        modalOverlay.setManaged(true);
    }

    private void hideModal() {
        modalMode = ModalMode.NONE;
        modalTargetTrip = null;
        modalOverlay.setVisible(false);
        modalOverlay.setManaged(false);
        clearModalError();
    }

    private void clearModalError() {
        modalErrorLabel.setText("");
        modalErrorLabel.setVisible(false);
        modalErrorLabel.setManaged(false);
    }

    private void showModalError(String message) {
        modalErrorLabel.setText(message);
        modalErrorLabel.setVisible(true);
        modalErrorLabel.setManaged(true);
    }

    private void showFeedback(String message, boolean isError) {
        feedbackLabel.setText(message);
        feedbackLabel.getStyleClass().removeAll("feedback-success", "feedback-error");
        feedbackLabel.getStyleClass().add(isError ? "feedback-error" : "feedback-success");
    }

    private void updateFilterLabels() {
        long acceptedCount = allTrips.stream().filter(t -> t.getStatus() == TripStatus.ACCEPTED).count();
        long inProgressCount = allTrips.stream().filter(t -> t.getStatus() == TripStatus.IN_PROGRESS).count();
        long completedCount = allTrips.stream().filter(t -> t.getStatus() == TripStatus.COMPLETED).count();
        long pendingCount = allTrips.stream().filter(t -> t.getStatus() == TripStatus.PENDING).count();

        if (filterAllButton != null)      filterAllButton.setText("Todos (" + allTrips.size() + ")");
        if (filterActiveButton != null)   filterActiveButton.setText("Aceites (" + acceptedCount + ")");
        if (filterInactiveButton != null) filterInactiveButton.setText("Em progresso (" + inProgressCount + ")");
        if (filterBlockedButton != null)  filterBlockedButton.setText("Concluidas (" + completedCount + ")");
        if (filterPendingButton != null)  filterPendingButton.setText("Pendentes (" + pendingCount + ")");

        listInfoLabel.setText("A mostrar " + filteredTrips.size() + " de " + allTrips.size() + " viagens");
    }

    private void updateFilterChipState() {
        setChipState(filterAllButton, currentStatusFilter == null);
        setChipState(filterActiveButton, currentStatusFilter == TripStatus.ACCEPTED);
        setChipState(filterInactiveButton, currentStatusFilter == TripStatus.IN_PROGRESS);
        setChipState(filterBlockedButton, currentStatusFilter == TripStatus.COMPLETED);
        setChipState(filterPendingButton, currentStatusFilter == TripStatus.PENDING);
    }

    private void setChipState(Button button, boolean active) {
        if (button == null) return;
        button.getStyleClass().removeAll("filter-chip", "filter-chip-active");
        button.getStyleClass().add(active ? "filter-chip-active" : "filter-chip");
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
