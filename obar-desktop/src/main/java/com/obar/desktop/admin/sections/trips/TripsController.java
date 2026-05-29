package com.obar.desktop.admin.sections.trips;

import com.obar.bll.admin.AdminService;
import com.obar.bll.admin.AdminTripCommand;
import com.obar.bll.admin.AdminTripDTO;
import com.obar.desktop.admin.sections.AdminSectionController;
import com.obar.desktop.admin.shared.AdminFormatUtils;
import com.obar.desktop.admin.shared.AdminModalIncludeController;
import com.obar.desktop.admin.shared.AdminParseUtils;
import com.obar.model.enums.TripStatus;
import com.obar.model.enums.TripType;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;

/**
 * Controller for the Trips admin section.
 *
 * <p>
 * Maintains the trip table state, applies status and search filters, handles
 * the trip form modal, and delegates create, update, and delete operations to
 * {@link AdminService}.
 * </p>
 */
public class TripsController implements AdminSectionController {

    private enum ModalMode {
        NONE, CREATE, EDIT, DELETE_CONFIRM
    }

    @FXML
    private TextField searchField;
    @FXML
    private TableView<AdminTripDTO> tripsTable;
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
    private Label listInfoLabel;
    @FXML
    private Label feedbackLabel;
    @FXML
    private Button allTripsFilterButton;
    @FXML
    private Button acceptedTripsFilterButton;
    @FXML
    private Button inProgressTripsFilterButton;
    @FXML
    private Button completedTripsFilterButton;
    @FXML
    private Button pendingTripsFilterButton;
    @FXML
    private Button addTripButton;
    @FXML
    private Button editTripButton;
    @FXML
    private Button deleteTripButton;

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

    private final ObservableList<AdminTripDTO> allTrips = FXCollections.observableArrayList();
    private final FilteredList<AdminTripDTO> filteredTrips = new FilteredList<>(allTrips, trip -> true);

    private AdminService adminService;
    private TripStatus activeStatusFilter;
    private ModalMode modalMode = ModalMode.NONE;
    private AdminTripDTO modalTarget;

    @Override
    public void setAdminService(AdminService adminService) {
        this.adminService = adminService;
    }

    @Override
    public void onSectionActivated() {
        reloadTrips();
    }

    @FXML
    public void initialize() {
        configureTripModal();
        configureTripTableColumns();

        tripsTable.setItems(filteredTrips);
        searchField.textProperty().addListener((obs, oldText, newText) -> applyTripFilter());
        tripsTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, previous, selected) -> showTripDetails(selected));
        allTrips.addListener((javafx.collections.ListChangeListener<AdminTripDTO>) change -> updateCountLabels());
        filteredTrips.addListener((javafx.collections.ListChangeListener<AdminTripDTO>) change -> updateCountLabels());

        setDetailVisible(false);
        updateFilterChipStyles();
        updateCountLabels();
    }

    private void configureTripModal() {
        sharedModalController.bindActions(this::handleModalCancel, this::handleModalSave,
                this::handleModalConfirmDelete);
        sharedModalController.<TripType>getModalTripTypeCombo()
                .setItems(FXCollections.observableArrayList(TripType.values()));
        sharedModalController.<TripStatus>getModalTripStatusCombo()
                .setItems(FXCollections.observableArrayList(TripStatus.values()));
    }

    @FXML
    public void handleShowAllTrips() {
        setStatusFilter(null);
    }

    @FXML
    public void handleShowAcceptedTrips() {
        setStatusFilter(TripStatus.ACCEPTED);
    }

    @FXML
    public void handleShowInProgressTrips() {
        setStatusFilter(TripStatus.IN_PROGRESS);
    }

    @FXML
    public void handleShowCompletedTrips() {
        setStatusFilter(TripStatus.COMPLETED);
    }

    @FXML
    public void handleShowPendingTrips() {
        setStatusFilter(TripStatus.PENDING);
    }

    @FXML
    public void handleAddTrip() {
        modalMode = ModalMode.CREATE;
        modalTarget = null;
        sharedModalController.prepareForForm("Nova viagem");
        showOnlyTripForm();
        clearTripForm();
    }

    @FXML
    public void handleEditTrip() {
        AdminTripDTO selected = tripsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showFeedback("Selecione uma viagem primeiro.", true);
            return;
        }
        modalMode = ModalMode.EDIT;
        modalTarget = selected;
        sharedModalController.prepareForForm("Editar viagem");
        showOnlyTripForm();
        fillTripForm(selected);
    }

    @FXML
    public void handleDeleteTrip() {
        AdminTripDTO selected = tripsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showFeedback("Selecione uma viagem primeiro.", true);
            return;
        }
        modalMode = ModalMode.DELETE_CONFIRM;
        modalTarget = selected;
        sharedModalController.prepareForDeleteConfirm("Apagar viagem");
        hideAllModalForms();
        sharedModalController.getModalDeleteMessageLabel().setText(
                "Tem a certeza que deseja apagar a viagem #" + selected.getId()
                        + "?\nRota: " + AdminFormatUtils.fallback(selected.getOriginAddress())
                        + " -> " + AdminFormatUtils.fallback(selected.getDestinationAddress()) + ".");
    }

    @FXML
    public void handleCloseDetailPanel() {
        tripsTable.getSelectionModel().clearSelection();
        setDetailVisible(false);
    }

    @FXML
    public void handleModalCancel() {
        closeModal();
    }

    @FXML
    public void handleModalSave() {
        if (modalMode == ModalMode.DELETE_CONFIRM) {
            return;
        }
        try {
            AdminTripCommand command = buildTripCommand();
            if (modalMode == ModalMode.EDIT) {
                if (modalTarget == null || modalTarget.getId() == null) {
                    sharedModalController.showError("Viagem invalida.");
                    return;
                }
                adminService.updateTrip(modalTarget.getId(), command);
                finishModalWithSuccess("Viagem atualizada com sucesso.");
                return;
            }

            adminService.createTrip(command);
            finishModalWithSuccess("Viagem criada com sucesso.");
        } catch (Exception exception) {
            sharedModalController.showError("Falha ao guardar viagem: " + exception.getMessage());
        }
    }

    @FXML
    public void handleModalConfirmDelete() {
        if (modalTarget == null || modalTarget.getId() == null) {
            sharedModalController.showError("Viagem invalida.");
            return;
        }
        try {
            adminService.deleteTrip(modalTarget.getId());
            finishModalWithSuccess("Viagem apagada com sucesso.");
        } catch (Exception exception) {
            sharedModalController.showError("Falha ao apagar viagem: " + exception.getMessage());
        }
    }

    private void reloadTrips() {
        if (adminService == null) {
            return;
        }
        allTrips.setAll(adminService.listTrips());
        applyTripFilter();
        updateCountLabels();
    }

    private void setStatusFilter(TripStatus status) {
        activeStatusFilter = status;
        applyTripFilter();
        updateFilterChipStyles();
        updateCountLabels();
    }

    private void applyTripFilter() {
        String query = AdminFormatUtils.normalize(searchField == null ? "" : searchField.getText());
        filteredTrips.setPredicate(trip -> matchesStatus(trip) && matchesSearch(trip, query));
    }

    private boolean matchesStatus(AdminTripDTO trip) {
        return activeStatusFilter == null || trip.getStatus() == activeStatusFilter;
    }

    private boolean matchesSearch(AdminTripDTO trip, String query) {
        if (query.isBlank()) {
            return true;
        }
        String id = trip.getId() == null ? "" : String.valueOf(trip.getId());
        return AdminFormatUtils.normalize(id).contains(query)
                || AdminFormatUtils.normalize(trip.getClientName()).contains(query)
                || AdminFormatUtils.normalize(trip.getDriverName()).contains(query)
                || AdminFormatUtils.normalize(trip.getOriginAddress()).contains(query)
                || AdminFormatUtils.normalize(trip.getDestinationAddress()).contains(query)
                || AdminFormatUtils.normalize(AdminFormatUtils.prettyTripStatus(trip.getStatus())).contains(query);
    }

    private void updateCountLabels() {
        setButtonText(allTripsFilterButton, "Todos (" + allTrips.size() + ")");
        setButtonText(acceptedTripsFilterButton, "Aceites (" + count(TripStatus.ACCEPTED) + ")");
        setButtonText(inProgressTripsFilterButton, "Em progresso (" + count(TripStatus.IN_PROGRESS) + ")");
        setButtonText(completedTripsFilterButton, "Concluidas (" + count(TripStatus.COMPLETED) + ")");
        setButtonText(pendingTripsFilterButton, "Pendentes (" + count(TripStatus.PENDING) + ")");
        setLabelText(listInfoLabel, "A mostrar " + filteredTrips.size() + " de " + allTrips.size() + " viagens");
    }

    private long count(TripStatus status) {
        return allTrips.stream().filter(trip -> trip.getStatus() == status).count();
    }

    private void updateFilterChipStyles() {
        updateChip(allTripsFilterButton, activeStatusFilter == null);
        updateChip(acceptedTripsFilterButton, activeStatusFilter == TripStatus.ACCEPTED);
        updateChip(inProgressTripsFilterButton, activeStatusFilter == TripStatus.IN_PROGRESS);
        updateChip(completedTripsFilterButton, activeStatusFilter == TripStatus.COMPLETED);
        updateChip(pendingTripsFilterButton, activeStatusFilter == TripStatus.PENDING);
    }

    private void updateChip(Button button, boolean active) {
        if (button == null) {
            return;
        }
        button.getStyleClass().removeAll("filter-chip", "filter-chip-active");
        button.getStyleClass().add(active ? "filter-chip-active" : "filter-chip");
    }

    private void configureTripTableColumns() {
        tripIdColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getId() == null ? "-" : "#" + cellData.getValue().getId()));
        tripClientColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                AdminFormatUtils.fallback(cellData.getValue().getClientName())));
        tripDriverColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                AdminFormatUtils.fallback(cellData.getValue().getDriverName())));
        tripStatusColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                AdminFormatUtils.prettyTripStatus(cellData.getValue().getStatus())));
        tripTypeColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                AdminFormatUtils.prettyTripType(cellData.getValue().getTripType())));
        tripPriceColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                AdminFormatUtils.formatTripPrice(cellData.getValue())));
        tripRequestedAtColumn.setCellValueFactory(cellData -> {
            LocalDateTime requestTime = cellData.getValue().getRequestTime();
            return new SimpleStringProperty(requestTime == null ? "-" : requestTime.toString());
        });

        tripStatusColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String statusLabel, boolean empty) {
                super.updateItem(statusLabel, empty);
                getStyleClass().removeAll(
                        "trip-completed", "trip-in-progress", "trip-accepted",
                        "trip-pending", "trip-cancelled", "trip-rejected");
                if (empty || statusLabel == null) {
                    setText(null);
                    return;
                }
                setText(statusLabel);
                AdminTripDTO trip = getIndex() >= 0 && getIndex() < getTableView().getItems().size()
                        ? getTableView().getItems().get(getIndex())
                        : null;
                if (trip == null || trip.getStatus() == null) {
                    return;
                }
                switch (trip.getStatus()) {
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

    private AdminTripCommand buildTripCommand() {
        return new AdminTripCommand(
                AdminParseUtils.parseRequiredInteger(sharedModalController.getModalTripClientIdField().getText(),
                        "Cliente ID"),
                AdminParseUtils.parseOptionalInteger(sharedModalController.getModalTripDriverIdField().getText()),
                sharedModalController.getModalTripOriginField().getText(),
                sharedModalController.getModalTripDestinationField().getText(),
                sharedModalController.<TripType>getModalTripTypeCombo().getValue(),
                sharedModalController.<TripStatus>getModalTripStatusCombo().getValue(),
                sharedModalController.getModalTripNotesField().getText(),
                AdminParseUtils.parseOptionalDecimal(sharedModalController.getModalTripEstimatedPriceField().getText()),
                AdminParseUtils.parseOptionalDecimal(sharedModalController.getModalTripFinalPriceField().getText()));
    }

    private void showOnlyTripForm() {
        AdminModalIncludeController.setVisible(sharedModalController.getModalUsersFormSection(), false);
        AdminModalIncludeController.setVisible(sharedModalController.getModalTripsFormSection(), true);
        AdminModalIncludeController.setVisible(sharedModalController.getModalTaxRateFormSection(), false);
        AdminModalIncludeController.setVisible(sharedModalController.getModalDeleteSection(), false);
    }

    private void hideAllModalForms() {
        AdminModalIncludeController.setVisible(sharedModalController.getModalUsersFormSection(), false);
        AdminModalIncludeController.setVisible(sharedModalController.getModalTripsFormSection(), false);
        AdminModalIncludeController.setVisible(sharedModalController.getModalTaxRateFormSection(), false);
    }

    private void fillTripForm(AdminTripDTO trip) {
        sharedModalController.getModalTripClientIdField()
                .setText(trip.getClientId() == null ? "" : String.valueOf(trip.getClientId()));
        sharedModalController.getModalTripDriverIdField()
                .setText(trip.getDriverId() == null ? "" : String.valueOf(trip.getDriverId()));
        sharedModalController.<TripType>getModalTripTypeCombo()
                .setValue(trip.getTripType() == null ? TripType.IMMEDIATE : trip.getTripType());
        sharedModalController.<TripStatus>getModalTripStatusCombo()
                .setValue(trip.getStatus() == null ? TripStatus.PENDING : trip.getStatus());
        sharedModalController.getModalTripOriginField().setText(nullToEmpty(trip.getOriginAddress()));
        sharedModalController.getModalTripDestinationField().setText(nullToEmpty(trip.getDestinationAddress()));
        sharedModalController.getModalTripEstimatedPriceField()
                .setText(trip.getEstimatedPrice() == null ? "" : trip.getEstimatedPrice().toPlainString());
        sharedModalController.getModalTripFinalPriceField()
                .setText(trip.getFinalPrice() == null ? "" : trip.getFinalPrice().toPlainString());
        sharedModalController.getModalTripNotesField().setText(nullToEmpty(trip.getNotes()));
    }

    private void clearTripForm() {
        sharedModalController.getModalTripClientIdField().clear();
        sharedModalController.getModalTripDriverIdField().clear();
        sharedModalController.<TripType>getModalTripTypeCombo().setValue(TripType.IMMEDIATE);
        sharedModalController.<TripStatus>getModalTripStatusCombo().setValue(TripStatus.PENDING);
        sharedModalController.getModalTripOriginField().clear();
        sharedModalController.getModalTripDestinationField().clear();
        sharedModalController.getModalTripEstimatedPriceField().clear();
        sharedModalController.getModalTripFinalPriceField().clear();
        sharedModalController.getModalTripNotesField().clear();
    }

    private void showTripDetails(AdminTripDTO trip) {
        if (trip == null) {
            setDetailVisible(false);
            return;
        }

        String route = AdminFormatUtils.fallback(trip.getOriginAddress())
                + " -> " + AdminFormatUtils.fallback(trip.getDestinationAddress());
        setLabelText(detailInitialsLabel, trip.getId() == null ? "--" : "#" + trip.getId());
        setLabelText(detailTitleLabel, "Detalhe da viagem");
        setLabelText(detailNameLabel, route);
        setLabelText(detailEmailLabel, "Cliente: " + AdminFormatUtils.fallback(trip.getClientName()));
        setLabelText(detailStatusLabel, AdminFormatUtils.prettyTripStatus(trip.getStatus()));
        setLabelText(detailRoleLabel, AdminFormatUtils.prettyTripType(trip.getTripType()));
        setLabelText(detailPhoneValueLabel, "Motorista: " + AdminFormatUtils.fallback(trip.getDriverName()));
        setLabelText(detailCreatedValueLabel, trip.getRequestTime() == null ? "-" : trip.getRequestTime().toString());
        setLabelText(detailCardOneTitleLabel, "ID Cliente");
        setLabelText(detailCardOneValueLabel, trip.getClientId() == null ? "-" : "#" + trip.getClientId());
        setLabelText(detailCardTwoTitleLabel, "ID Motorista");
        setLabelText(detailCardTwoValueLabel, trip.getDriverId() == null ? "-" : "#" + trip.getDriverId());
        setLabelText(detailCardThreeTitleLabel, "Veiculo");
        setLabelText(detailCardThreeValueLabel, AdminFormatUtils.fallback(trip.getVehicleDisplay()));
        setLabelText(detailCardFourTitleLabel, "Distancia");
        setLabelText(detailCardFourValueLabel, trip.getDistanceKm() == null ? "-" : trip.getDistanceKm() + " km");
        setLabelText(detailReferenceTitleLabel, "Preco estimado");
        setLabelText(detailReferenceValueLabel,
                trip.getEstimatedPrice() == null ? "-" : "EUR " + trip.getEstimatedPrice());
        setLabelText(detailExtraOneTitleLabel, "Preco final");
        setLabelText(detailExtraOneValueLabel, trip.getFinalPrice() == null ? "-" : "EUR " + trip.getFinalPrice());
        setLabelText(detailExtraTwoTitleLabel, "Inicio");
        setLabelText(detailExtraTwoValueLabel, trip.getStartTime() == null ? "-" : trip.getStartTime().toString());
        setLabelText(detailExtraThreeTitleLabel, "Fim");
        setLabelText(detailExtraThreeValueLabel, trip.getEndTime() == null ? "-" : trip.getEndTime().toString());
        setDetailVisible(true);
    }

    private void finishModalWithSuccess(String message) {
        closeModal();
        reloadTrips();
        showFeedback(message, false);
    }

    private void closeModal() {
        modalMode = ModalMode.NONE;
        modalTarget = null;
        sharedModalController.hide();
    }

    private void showFeedback(String message, boolean isError) {
        if (feedbackLabel == null) {
            return;
        }
        feedbackLabel.setText(message == null ? "" : message);
        feedbackLabel.getStyleClass().removeAll("feedback-success", "feedback-error");
        feedbackLabel.getStyleClass().add(isError ? "feedback-error" : "feedback-success");
    }

    private void setDetailVisible(boolean visible) {
        if (detailPanel != null) {
            detailPanel.setVisible(visible);
            detailPanel.setManaged(visible);
        }
    }

    private void setButtonText(Button button, String text) {
        if (button != null) {
            button.setText(text);
        }
    }

    private void setLabelText(Label label, String text) {
        if (label != null) {
            label.setText(text == null ? "-" : text);
        }
    }

    private String nullToEmpty(String value) {
        return value == null ? "" : value;
    }
}
