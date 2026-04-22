package com.obar.desktop.admin.sections.trips;

import com.obar.bll.admin.AdminService;
import com.obar.bll.admin.AdminTripCommand;
import com.obar.bll.admin.AdminTripDTO;
import com.obar.desktop.admin.sections.AdminSectionController;
import com.obar.desktop.admin.shared.AdminFormatUtils;
import com.obar.desktop.admin.shared.AdminModalController;
import com.obar.desktop.admin.shared.AdminModalIncludeController;
import com.obar.desktop.admin.shared.AdminParseUtils;
import com.obar.desktop.admin.shared.DetailPanelBinder;
import com.obar.desktop.admin.shared.DetailPanelBinder.DetailViewModel;
import com.obar.desktop.admin.shared.FilterChipManager;
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
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;

public class TripsController implements AdminSectionController {

    private enum ModalMode { NONE, CREATE, EDIT, DELETE_CONFIRM }

    private final ObservableList<AdminTripDTO> allTrips = FXCollections.observableArrayList();
    private final FilteredList<AdminTripDTO> filteredTrips = new FilteredList<>(allTrips, trip -> true);

    private AdminService adminService;
    private TripStatus currentStatusFilter;
    private ModalMode modalMode = ModalMode.NONE;
    private AdminTripDTO modalTargetTrip;
    private AdminModalController modalController;
    private DetailPanelBinder detailBinder;
    private FilterChipManager<TripStatus> statusChips;

    // — table & toolbar —
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
    @FXML private Button filterAllButton;
    @FXML private Button filterActiveButton;
    @FXML private Button filterInactiveButton;
    @FXML private Button filterBlockedButton;
    @FXML private Button filterPendingButton;

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
    @FXML private VBox modalTripsFormSection;
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

        statusChips = new FilterChipManager<TripStatus>()
                .add(filterAllButton,      null)
                .add(filterActiveButton,   TripStatus.ACCEPTED)
                .add(filterInactiveButton, TripStatus.IN_PROGRESS)
                .add(filterBlockedButton,  TripStatus.COMPLETED)
                .add(filterPendingButton,  TripStatus.PENDING);

        tripsTable.setItems(filteredTrips);
        modalTripTypeCombo.setItems(FXCollections.observableArrayList(TripType.values()));
        modalTripStatusCombo.setItems(FXCollections.observableArrayList(TripStatus.values()));
        searchField.textProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        tripsTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, previous, current) -> onSelectionChanged(current));
        setupColumns();
        detailBinder.setVisible(false);
        refresh();
    }

    private void bindModalFields() {
        if (sharedModalController == null) {
            throw new IllegalStateException("Shared modal controller was not injected.");
        }
        modalTripsFormSection      = sharedModalController.getModalTripsFormSection();
        modalDeleteMessageLabel    = sharedModalController.getModalDeleteMessageLabel();
        modalTripClientIdField     = sharedModalController.getModalTripClientIdField();
        modalTripDriverIdField     = sharedModalController.getModalTripDriverIdField();
        modalTripTypeCombo         = sharedModalController.getModalTripTypeCombo();
        modalTripStatusCombo       = sharedModalController.getModalTripStatusCombo();
        modalTripOriginField       = sharedModalController.getModalTripOriginField();
        modalTripDestinationField  = sharedModalController.getModalTripDestinationField();
        modalTripEstimatedPriceField = sharedModalController.getModalTripEstimatedPriceField();
        modalTripFinalPriceField   = sharedModalController.getModalTripFinalPriceField();
        modalTripNotesField        = sharedModalController.getModalTripNotesField();
    }

    @Override
    public void onSectionActivated() {
        refresh();
    }

    // — filter handlers —

    @FXML public void handleFilterAll()      { setStatusFilter(null); }
    @FXML public void handleFilterActive()   { setStatusFilter(TripStatus.ACCEPTED); }
    @FXML public void handleFilterInactive() { setStatusFilter(TripStatus.IN_PROGRESS); }
    @FXML public void handleFilterBlocked()  { setStatusFilter(TripStatus.COMPLETED); }
    @FXML public void handleFilterPending()  { setStatusFilter(TripStatus.PENDING); }

    // — toolbar handlers —

    @FXML
    public void handleAddUser() {
        openTripFormModal(null);
    }

    @FXML
    public void handleEditUser() {
        AdminTripDTO selected = tripsTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showFeedback("Selecione uma viagem primeiro.", true); return; }
        openTripFormModal(selected);
    }

    @FXML
    public void handleDeleteUser() {
        AdminTripDTO selected = tripsTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showFeedback("Selecione uma viagem primeiro.", true); return; }
        openDeleteTripConfirmModal(selected);
    }

    @FXML public void handleCloseDetailPanel() {
        tripsTable.getSelectionModel().clearSelection();
        detailBinder.clear(emptyState());
    }

    // — modal handlers —

    @FXML public void handleModalCancel() { hideModal(); }

    @FXML
    public void handleModalSave() {
        if (modalMode != ModalMode.DELETE_CONFIRM) {
            persistTrip();
        }
    }

    @FXML
    public void handleModalConfirmDelete() {
        if (modalMode != ModalMode.DELETE_CONFIRM || modalTargetTrip == null) { hideModal(); return; }
        try {
            if (modalTargetTrip.getId() == null) { showModalError("Viagem invalida."); return; }
            adminService.deleteTrip(modalTargetTrip.getId());
            hideModal();
            refresh();
            showFeedback("Viagem apagada com sucesso.", false);
        } catch (Exception exception) {
            showModalError("Falha ao apagar viagem: " + exception.getMessage());
        }
    }

    // — private helpers —

    private void setStatusFilter(TripStatus status) {
        currentStatusFilter = status;
        statusChips.setActive(status);
        applyFilters();
    }

    private void refresh() {
        if (adminService == null) { return; }
        allTrips.setAll(adminService.listTrips());
        applyFilters();
        updateFilterLabels();
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
        if (query.isBlank()) { return true; }
        String idValue = trip.getId() == null ? "" : String.valueOf(trip.getId());
        return AdminFormatUtils.normalize(idValue).contains(query)
                || AdminFormatUtils.normalize(trip.getClientName()).contains(query)
                || AdminFormatUtils.normalize(trip.getDriverName()).contains(query)
                || AdminFormatUtils.normalize(AdminFormatUtils.prettyTripStatus(trip.getStatus())).contains(query)
                || AdminFormatUtils.normalize(AdminFormatUtils.prettyTripType(trip.getTripType())).contains(query)
                || AdminFormatUtils.normalize(trip.getOriginAddress()).contains(query)
                || AdminFormatUtils.normalize(trip.getDestinationAddress()).contains(query);
    }

    private void onSelectionChanged(AdminTripDTO trip) {
        if (trip == null) { detailBinder.clear(emptyState()); return; }
        detailBinder.bind(new DetailViewModel.Builder()
                .initials(trip.getId() == null ? "--" : "#" + trip.getId())
                .title("Detalhe da viagem")
                .name(AdminFormatUtils.fallback(trip.getOriginAddress()) + " -> " + AdminFormatUtils.fallback(trip.getDestinationAddress()))
                .email("Cliente: " + AdminFormatUtils.fallback(trip.getClientName()))
                .status(AdminFormatUtils.prettyTripStatus(trip.getStatus()))
                .role(AdminFormatUtils.prettyTripType(trip.getTripType()))
                .phone("Motorista: " + AdminFormatUtils.fallback(trip.getDriverName()))
                .created(trip.getRequestTime() == null ? "-" : trip.getRequestTime().toString())
                .card1("ID Cliente",      trip.getClientId() == null ? "-" : "#" + trip.getClientId())
                .card2("ID Motorista",    trip.getDriverId() == null ? "-" : "#" + trip.getDriverId())
                .card3("Veiculo",         AdminFormatUtils.fallback(trip.getVehicleDisplay()))
                .card4("Distancia",       trip.getDistanceKm() == null ? "-" : trip.getDistanceKm() + " km")
                .ref("Preco estimado",    trip.getEstimatedPrice() == null ? "-" : "EUR " + trip.getEstimatedPrice())
                .extra1("Preco final",    trip.getFinalPrice() == null ? "-" : "EUR " + trip.getFinalPrice())
                .extra2("Inicio",         trip.getStartTime() == null ? "-" : trip.getStartTime().toString())
                .extra3("Fim",            trip.getEndTime() == null ? "-" : trip.getEndTime().toString())
                .build());
    }

    private static DetailViewModel emptyState() {
        return new DetailViewModel.Builder()
                .title("Sem selecao").name("Sem selecao")
                .card1("ID Cliente", "-").card2("ID Motorista", "-")
                .card3("Veiculo", "-").card4("Distancia", "-")
                .ref("Preco estimado", "-")
                .extra1("Preco final", "-").extra2("Inicio", "-").extra3("Fim", "-")
                .build();
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
            LocalDateTime t = cellData.getValue().getRequestTime();
            return new SimpleStringProperty(t == null ? "-" : t.toString());
        });
        tripStatusColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("trip-completed", "trip-in-progress", "trip-accepted",
                        "trip-pending", "trip-cancelled", "trip-rejected");
                if (empty || item == null) { setText(null); return; }
                setText(item);
                if (getIndex() < 0 || getIndex() >= getTableView().getItems().size()) { return; }
                AdminTripDTO row = getTableView().getItems().get(getIndex());
                if (row.getStatus() == null) { return; }
                switch (row.getStatus()) {
                    case COMPLETED   -> getStyleClass().add("trip-completed");
                    case IN_PROGRESS -> getStyleClass().add("trip-in-progress");
                    case ACCEPTED    -> getStyleClass().add("trip-accepted");
                    case PENDING     -> getStyleClass().add("trip-pending");
                    case CANCELLED   -> getStyleClass().add("trip-cancelled");
                    case REJECTED    -> getStyleClass().add("trip-rejected");
                }
            }
        });
    }

    private void updateFilterLabels() {
        long accepted   = allTrips.stream().filter(t -> t.getStatus() == TripStatus.ACCEPTED).count();
        long inProgress = allTrips.stream().filter(t -> t.getStatus() == TripStatus.IN_PROGRESS).count();
        long completed  = allTrips.stream().filter(t -> t.getStatus() == TripStatus.COMPLETED).count();
        long pending    = allTrips.stream().filter(t -> t.getStatus() == TripStatus.PENDING).count();

        if (filterAllButton != null)      filterAllButton.setText("Todos (" + allTrips.size() + ")");
        if (filterActiveButton != null)   filterActiveButton.setText("Aceites (" + accepted + ")");
        if (filterInactiveButton != null) filterInactiveButton.setText("Em progresso (" + inProgress + ")");
        if (filterBlockedButton != null)  filterBlockedButton.setText("Concluidas (" + completed + ")");
        if (filterPendingButton != null)  filterPendingButton.setText("Pendentes (" + pending + ")");
        listInfoLabel.setText("A mostrar " + filteredTrips.size() + " de " + allTrips.size() + " viagens");
    }

    private void openTripFormModal(AdminTripDTO editingTrip) {
        boolean editing = editingTrip != null;
        modalMode = editing ? ModalMode.EDIT : ModalMode.CREATE;
        modalTargetTrip = editingTrip;
        modalController.prepareForForm(editing ? "Editar viagem" : "Nova viagem");
        AdminModalController.toggle(modalTripsFormSection, true);

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
    }

    private void openDeleteTripConfirmModal(AdminTripDTO selectedTrip) {
        modalMode = ModalMode.DELETE_CONFIRM;
        modalTargetTrip = selectedTrip;
        modalController.prepareForDeleteConfirm("Apagar viagem");
        AdminModalController.toggle(modalTripsFormSection, false);
        modalDeleteMessageLabel.setText("Tem a certeza que deseja apagar a viagem #" + selectedTrip.getId()
                + "?\nRota: " + AdminFormatUtils.fallback(selectedTrip.getOriginAddress())
                + " -> " + AdminFormatUtils.fallback(selectedTrip.getDestinationAddress()) + ".");
    }

    private void persistTrip() {
        try {
            AdminTripCommand command = new AdminTripCommand(
                    AdminParseUtils.parseRequiredInteger(modalTripClientIdField.getText(), "Cliente ID"),
                    AdminParseUtils.parseOptionalInteger(modalTripDriverIdField.getText()),
                    modalTripOriginField.getText(),
                    modalTripDestinationField.getText(),
                    modalTripTypeCombo.getValue(),
                    modalTripStatusCombo.getValue(),
                    modalTripNotesField.getText(),
                    AdminParseUtils.parseOptionalDecimal(modalTripEstimatedPriceField.getText()),
                    AdminParseUtils.parseOptionalDecimal(modalTripFinalPriceField.getText()));

            if (modalMode == ModalMode.EDIT) {
                if (modalTargetTrip == null || modalTargetTrip.getId() == null) { showModalError("Viagem invalida."); return; }
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

    private void hideModal() {
        modalMode = ModalMode.NONE;
        modalTargetTrip = null;
        modalController.hide();
    }

    private void showModalError(String message) { modalController.showError(message); }
    private void showFeedback(String message, boolean isError) {
        feedbackLabel.setText(message);
        feedbackLabel.getStyleClass().removeAll("feedback-success", "feedback-error");
        feedbackLabel.getStyleClass().add(isError ? "feedback-error" : "feedback-success");
    }
}
