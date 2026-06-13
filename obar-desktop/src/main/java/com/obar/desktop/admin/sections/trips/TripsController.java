package com.obar.desktop.admin.sections.trips;

import static com.obar.desktop.admin.shared.AdminPdfExportService.column;

import com.obar.bll.admin.AdminService;
import com.obar.bll.admin.AdminTripCommand;
import com.obar.bll.admin.AdminTripDTO;
import com.obar.desktop.admin.sections.AdminSectionController;
import com.obar.desktop.admin.shared.AdminFormatUtils;
import com.obar.desktop.admin.shared.AdminModalIncludeController;
import com.obar.desktop.admin.shared.AdminPdfExportService;
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
import java.time.format.DateTimeFormatter;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

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

    private static final DateTimeFormatter EXPORT_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm",
            Locale.forLanguageTag("pt-PT"));

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
    private final AdminPdfExportService pdfExportService = new AdminPdfExportService();

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
        sharedModalController.modalTripTypeCombo
                .setItems(FXCollections.observableArrayList(TripType.values()));
        sharedModalController.modalTripStatusCombo
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
        sharedModalController.modalDeleteMessageLabel.setText(
                "Tem a certeza que deseja apagar a viagem #" + selected.id()
                        + "?\nRota: " + AdminFormatUtils.fallback(selected.originAddress())
                        + " -> " + AdminFormatUtils.fallback(selected.destinationAddress()) + ".");
    }

    @FXML
    public void handleExportPdf() {
        try {
            Path exportPath = pdfExportService.exportTable(
                    "OBAR - Viagens",
                    "Dados atualmente mostrados na dashboard: " + filteredTrips.size() + " de " + allTrips.size(),
                    "admin_viagens",
                    tripExportColumns(),
                    List.copyOf(filteredTrips));
            showFeedback("PDF exportado: " + exportPath.toAbsolutePath(), false);
        } catch (Exception exception) {
            showFeedback("Falha ao exportar PDF: " + exception.getMessage(), true);
        }
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
                if (modalTarget == null || modalTarget.id() == null) {
                    sharedModalController.showError("Viagem invalida.");
                    return;
                }
                adminService.updateTrip(modalTarget.id(), command);
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
        if (modalTarget == null || modalTarget.id() == null) {
            sharedModalController.showError("Viagem invalida.");
            return;
        }
        try {
            adminService.deleteTrip(modalTarget.id());
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
        return activeStatusFilter == null || trip.status() == activeStatusFilter;
    }

    private boolean matchesSearch(AdminTripDTO trip, String query) {
        if (query.isBlank()) {
            return true;
        }
        String id = trip.id() == null ? "" : String.valueOf(trip.id());
        return AdminFormatUtils.normalize(id).contains(query)
                || AdminFormatUtils.normalize(trip.clientName()).contains(query)
                || AdminFormatUtils.normalize(trip.driverName()).contains(query)
                || AdminFormatUtils.normalize(trip.originAddress()).contains(query)
                || AdminFormatUtils.normalize(trip.destinationAddress()).contains(query)
                || AdminFormatUtils.normalize(AdminFormatUtils.prettyTripStatus(trip.status())).contains(query);
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
        return allTrips.stream().filter(trip -> trip.status() == status).count();
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
                cellData.getValue().id() == null ? "-" : "#" + cellData.getValue().id()));
        tripClientColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                AdminFormatUtils.fallback(cellData.getValue().clientName())));
        tripDriverColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                AdminFormatUtils.fallback(cellData.getValue().driverName())));
        tripStatusColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                AdminFormatUtils.prettyTripStatus(cellData.getValue().status())));
        tripTypeColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                AdminFormatUtils.prettyTripType(cellData.getValue().tripType())));
        tripPriceColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                AdminFormatUtils.formatTripPrice(cellData.getValue())));
        tripRequestedAtColumn.setCellValueFactory(cellData -> {
            LocalDateTime requestTime = cellData.getValue().requestTime();
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
                if (trip == null || trip.status() == null) {
                    return;
                }
                switch (trip.status()) {
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
                AdminFormatUtils.parseRequiredInteger(sharedModalController.modalTripClientIdField.getText(),
                        "Cliente ID"),
                AdminFormatUtils.parseOptionalInteger(sharedModalController.modalTripDriverIdField.getText(),
                        "Motorista ID"),
                sharedModalController.modalTripOriginField.getText(),
                sharedModalController.modalTripDestinationField.getText(),
                sharedModalController.modalTripTypeCombo.getValue(),
                sharedModalController.modalTripStatusCombo.getValue(),
                sharedModalController.modalTripNotesField.getText(),
                AdminFormatUtils.parseOptionalDecimal(
                        sharedModalController.modalTripEstimatedPriceField.getText(), "Preco estimado"),
                AdminFormatUtils.parseOptionalDecimal(
                        sharedModalController.modalTripFinalPriceField.getText(), "Preco final"));
    }

    private void showOnlyTripForm() {
        AdminModalIncludeController.setVisible(sharedModalController.modalUsersFormSection, false);
        AdminModalIncludeController.setVisible(sharedModalController.modalTripsFormSection, true);
        AdminModalIncludeController.setVisible(sharedModalController.modalTaxRateFormSection, false);
        AdminModalIncludeController.setVisible(sharedModalController.modalDeleteSection, false);
    }

    private void hideAllModalForms() {
        AdminModalIncludeController.setVisible(sharedModalController.modalUsersFormSection, false);
        AdminModalIncludeController.setVisible(sharedModalController.modalTripsFormSection, false);
        AdminModalIncludeController.setVisible(sharedModalController.modalTaxRateFormSection, false);
    }

    private void fillTripForm(AdminTripDTO trip) {
        sharedModalController.modalTripClientIdField
                .setText(trip.clientId() == null ? "" : String.valueOf(trip.clientId()));
        sharedModalController.modalTripDriverIdField
                .setText(trip.driverId() == null ? "" : String.valueOf(trip.driverId()));
        sharedModalController.modalTripTypeCombo
                .setValue(trip.tripType() == null ? TripType.IMMEDIATE : trip.tripType());
        sharedModalController.modalTripStatusCombo
                .setValue(trip.status() == null ? TripStatus.PENDING : trip.status());
        sharedModalController.modalTripOriginField.setText(nullToEmpty(trip.originAddress()));
        sharedModalController.modalTripDestinationField.setText(nullToEmpty(trip.destinationAddress()));
        sharedModalController.modalTripEstimatedPriceField
                .setText(trip.estimatedPrice() == null ? "" : trip.estimatedPrice().toPlainString());
        sharedModalController.modalTripFinalPriceField
                .setText(trip.finalPrice() == null ? "" : trip.finalPrice().toPlainString());
        sharedModalController.modalTripNotesField.setText(nullToEmpty(trip.notes()));
    }

    private void clearTripForm() {
        sharedModalController.modalTripClientIdField.clear();
        sharedModalController.modalTripDriverIdField.clear();
        sharedModalController.modalTripTypeCombo.setValue(TripType.IMMEDIATE);
        sharedModalController.modalTripStatusCombo.setValue(TripStatus.PENDING);
        sharedModalController.modalTripOriginField.clear();
        sharedModalController.modalTripDestinationField.clear();
        sharedModalController.modalTripEstimatedPriceField.clear();
        sharedModalController.modalTripFinalPriceField.clear();
        sharedModalController.modalTripNotesField.clear();
    }

    private void showTripDetails(AdminTripDTO trip) {
        if (trip == null) {
            setDetailVisible(false);
            return;
        }

        String route = AdminFormatUtils.fallback(trip.originAddress())
                + " -> " + AdminFormatUtils.fallback(trip.destinationAddress());
        setLabelText(detailInitialsLabel, trip.id() == null ? "--" : "#" + trip.id());
        setLabelText(detailTitleLabel, "Detalhe da viagem");
        setLabelText(detailNameLabel, route);
        setLabelText(detailEmailLabel, "Cliente: " + AdminFormatUtils.fallback(trip.clientName()));
        setLabelText(detailStatusLabel, AdminFormatUtils.prettyTripStatus(trip.status()));
        setLabelText(detailRoleLabel, AdminFormatUtils.prettyTripType(trip.tripType()));
        setLabelText(detailPhoneValueLabel, "Motorista: " + AdminFormatUtils.fallback(trip.driverName()));
        setLabelText(detailCreatedValueLabel, trip.requestTime() == null ? "-" : trip.requestTime().toString());
        setLabelText(detailCardOneTitleLabel, "ID Cliente");
        setLabelText(detailCardOneValueLabel, trip.clientId() == null ? "-" : "#" + trip.clientId());
        setLabelText(detailCardTwoTitleLabel, "ID Motorista");
        setLabelText(detailCardTwoValueLabel, trip.driverId() == null ? "-" : "#" + trip.driverId());
        setLabelText(detailCardThreeTitleLabel, "Veiculo");
        setLabelText(detailCardThreeValueLabel, AdminFormatUtils.fallback(trip.vehicleDisplay()));
        setLabelText(detailCardFourTitleLabel, "Distancia");
        setLabelText(detailCardFourValueLabel, trip.distanceKm() == null ? "-" : trip.distanceKm() + " km");
        setLabelText(detailReferenceTitleLabel, "Preco estimado");
        setLabelText(detailReferenceValueLabel,
                trip.estimatedPrice() == null ? "-" : "EUR " + trip.estimatedPrice());
        setLabelText(detailExtraOneTitleLabel, "Preco final");
        setLabelText(detailExtraOneValueLabel, trip.finalPrice() == null ? "-" : "EUR " + trip.finalPrice());
        setLabelText(detailExtraTwoTitleLabel, "Inicio");
        setLabelText(detailExtraTwoValueLabel, trip.startTime() == null ? "-" : trip.startTime().toString());
        setLabelText(detailExtraThreeTitleLabel, "Fim");
        setLabelText(detailExtraThreeValueLabel, trip.endTime() == null ? "-" : trip.endTime().toString());
        setDetailVisible(true);
    }

    private List<AdminPdfExportService.PdfColumn<AdminTripDTO>> tripExportColumns() {
        return List.of(
                column("ID", 0.6f, trip -> formatId(trip.id())),
                column("Cliente ID", 0.8f, trip -> formatId(trip.clientId())),
                column("Cliente", 1.4f, AdminTripDTO::clientName),
                column("Motorista ID", 0.8f, trip -> formatId(trip.driverId())),
                column("Motorista", 1.4f, AdminTripDTO::driverName),
                column("Veiculo ID", 0.8f, trip -> formatId(trip.vehicleId())),
                column("Marca", 1f, AdminTripDTO::vehicleBrand),
                column("Modelo", 1f, AdminTripDTO::vehicleModel),
                column("Matricula", 1f, AdminTripDTO::vehicleLicensePlate),
                column("Categoria", 0.9f, AdminTripDTO::vehicleCategory),
                column("Estado", 1f, trip -> AdminFormatUtils.prettyTripStatus(trip.status())),
                column("Tipo", 0.9f, trip -> AdminFormatUtils.prettyTripType(trip.tripType())),
                column("Estimado", 0.9f, trip -> formatMoney(trip.estimatedPrice())),
                column("Final", 0.9f, trip -> formatMoney(trip.finalPrice())),
                column("Distancia", 0.9f, trip -> formatDistance(trip.distanceKm())),
                column("Origem", 2.1f, AdminTripDTO::originAddress),
                column("Destino", 2.1f, AdminTripDTO::destinationAddress),
                column("Pedido em", 1.4f, trip -> formatDate(trip.requestTime())),
                column("Inicio", 1.4f, trip -> formatDate(trip.startTime())),
                column("Fim", 1.4f, trip -> formatDate(trip.endTime())),
                column("Cancelado por", 1.1f, AdminTripDTO::cancelledBy),
                column("Motivo cancel.", 1.4f, AdminTripDTO::cancelReason),
                column("Notas", 1.5f, AdminTripDTO::notes));
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

    private String formatId(Integer id) {
        return id == null ? "-" : "#" + id;
    }

    private String formatDate(LocalDateTime value) {
        return value == null ? "-" : EXPORT_DATE_FORMAT.format(value);
    }

    private String formatDistance(Float distanceKm) {
        return distanceKm == null ? "-" : distanceKm + " km";
    }

    private String formatMoney(java.math.BigDecimal value) {
        return value == null ? "-" : "EUR " + value;
    }
}
