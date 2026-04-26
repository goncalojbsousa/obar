package com.obar.desktop.admin.sections.trips;

import com.obar.bll.admin.AdminService;
import com.obar.bll.admin.AdminTripDTO;
import com.obar.desktop.admin.sections.AdminSectionController;
import com.obar.desktop.admin.shared.AdminFormatUtils;
import com.obar.desktop.admin.shared.AdminModalController;
import com.obar.desktop.admin.shared.AdminModalIncludeController;
import com.obar.desktop.admin.shared.DetailPanelBinder;
import com.obar.desktop.admin.shared.FilterChipManager;
import com.obar.desktop.admin.sections.trips.TripModalPresenter.PersistResult;
import com.obar.model.enums.TripStatus;
import com.obar.model.enums.TripType;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
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
 * Responsibilities (only these):
 * <ul>
 * <li>Wire FXML nodes to the ViewModel and Presenter on {@code initialize()}.
 * <li>Dispatch user actions to {@link TripsViewModel} or
 * {@link TripModalPresenter}.
 * <li>React to ViewModel state changes (selection, filter counts) to update
 * labels.
 * </ul>
 *
 * <p>
 * No filtering logic, no formatting logic, no persistence logic lives here.
 */
public class TripsController implements AdminSectionController {

    // - table & toolbar -
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
    private Button filterAllButton;
    @FXML
    private Button filterActiveButton;
    @FXML
    private Button filterInactiveButton;
    @FXML
    private Button filterBlockedButton;
    @FXML
    private Button filterPendingButton;

    // - detail panel -
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

    // - shared modal include -
    @FXML
    private AdminModalIncludeController sharedModalController;

    // - collaborators (created in initialize, not injected) -
    private TripsViewModel viewModel;
    private TripModalPresenter modalPresenter;
    private DetailPanelBinder detailBinder;
    private FilterChipManager<TripStatus> statusChips;

    // ---------------------------------------------
    // AdminSectionController contract
    // ---------------------------------------------

    @Override
    public void setAdminService(AdminService adminService) {
        // ViewModel is created in initialize(); service is set here if called after,
        // or held until initialize() picks it up via the viewModel reference.
        if (viewModel != null) {
            viewModel.init(adminService);
        }
    }

    @Override
    public void onSectionActivated() {
        if (viewModel != null) {
            viewModel.reload();
            updateCountLabels();
        }
    }

    // ---------------------------------------------
    // Initialization
    // ---------------------------------------------

    @FXML
    public void initialize() {
        viewModel = new TripsViewModel();

        // Build modal infrastructure
        AdminModalController modalController = sharedModalController.createModalController();
        sharedModalController.bindActions(this::handleModalCancel, this::handleModalSave,
                this::handleModalConfirmDelete);

        // Build presenter and wire modal fields from shared include
        modalPresenter = new TripModalPresenter(viewModel, modalController);
        wireModalPresenterFields();

        // Build detail binder
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

        // Bind filter chips
        statusChips = new FilterChipManager<TripStatus>()
                .add(filterAllButton, null)
                .add(filterActiveButton, TripStatus.ACCEPTED)
                .add(filterInactiveButton, TripStatus.IN_PROGRESS)
                .add(filterBlockedButton, TripStatus.COMPLETED)
                .add(filterPendingButton, TripStatus.PENDING);

        // Populate combo boxes in shared modal
        sharedModalController.<TripType>getModalTripTypeCombo()
                .setItems(FXCollections.observableArrayList(TripType.values()));
        sharedModalController.<TripStatus>getModalTripStatusCombo()
                .setItems(FXCollections.observableArrayList(TripStatus.values()));

        // Bind table to filtered list
        tripsTable.setItems(viewModel.getFilteredTrips());

        // Bind search field to ViewModel property
        searchField.textProperty().bindBidirectional(viewModel.searchQueryProperty());

        // React to selection changes
        tripsTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, previous, current) -> onSelectionChanged(current));

        // React to filter changes to update chip styles and count labels
        viewModel.activeStatusFilterProperty()
                .addListener((obs, previous, current) -> {
                    statusChips.setActive(current);
                    updateCountLabels();
                });

        // React to list changes to update count labels
        viewModel.getAllTrips()
                .addListener((javafx.collections.ListChangeListener<AdminTripDTO>) change -> updateCountLabels());

        setupColumns();
        detailBinder.setVisible(false);
    }

    private void wireModalPresenterFields() {
        modalPresenter.wireFields(
                sharedModalController.getModalTripsFormSection(),
                sharedModalController.getModalDeleteMessageLabel(),
                sharedModalController.getModalTripClientIdField(),
                sharedModalController.getModalTripDriverIdField(),
                sharedModalController.getModalTripTypeCombo(),
                sharedModalController.getModalTripStatusCombo(),
                sharedModalController.getModalTripOriginField(),
                sharedModalController.getModalTripDestinationField(),
                sharedModalController.getModalTripEstimatedPriceField(),
                sharedModalController.getModalTripFinalPriceField(),
                sharedModalController.getModalTripNotesField());
    }

    // ---------------------------------------------
    // Filter handlers
    // ---------------------------------------------

    @FXML
    public void handleFilterAll() {
        viewModel.setStatusFilter(null);
    }

    @FXML
    public void handleFilterActive() {
        viewModel.setStatusFilter(TripStatus.ACCEPTED);
    }

    @FXML
    public void handleFilterInactive() {
        viewModel.setStatusFilter(TripStatus.IN_PROGRESS);
    }

    @FXML
    public void handleFilterBlocked() {
        viewModel.setStatusFilter(TripStatus.COMPLETED);
    }

    @FXML
    public void handleFilterPending() {
        viewModel.setStatusFilter(TripStatus.PENDING);
    }

    // ---------------------------------------------
    // Toolbar handlers
    // ---------------------------------------------

    @FXML
    public void handleAddUser() {
        modalPresenter.openCreate();
    }

    @FXML
    public void handleEditUser() {
        AdminTripDTO selected = tripsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showFeedback("Selecione uma viagem primeiro.", true);
            return;
        }
        modalPresenter.openEdit(selected);
    }

    @FXML
    public void handleDeleteUser() {
        AdminTripDTO selected = tripsTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showFeedback("Selecione uma viagem primeiro.", true);
            return;
        }
        modalPresenter.openDeleteConfirm(selected);
    }

    @FXML
    public void handleCloseDetailPanel() {
        tripsTable.getSelectionModel().clearSelection();
        detailBinder.clear(TripDetailMapper.empty());
    }

    // ---------------------------------------------
    // Modal handlers
    // ---------------------------------------------

    @FXML
    public void handleModalCancel() {
        modalPresenter.close();
    }

    @FXML
    public void handleModalSave() {
        TripsViewModel.ModalMode mode = viewModel.getModalMode();
        if (mode == TripsViewModel.ModalMode.DELETE_CONFIRM) {
            return;
        }
        PersistResult result = modalPresenter.save();
        if (result.success()) {
            modalPresenter.close();
            viewModel.reload();
            updateCountLabels();
            showFeedback(result.message(), false);
        } else {
            // Error is shown inside modal - presenter could also expose
            // modalController.showError(),
            // but keeping it simple: feedback bar is enough for non-modal errors.
            showFeedback(result.message(), true);
        }
    }

    @FXML
    public void handleModalConfirmDelete() {
        PersistResult result = modalPresenter.confirmDelete();
        if (result.success()) {
            modalPresenter.close();
            viewModel.reload();
            updateCountLabels();
            showFeedback(result.message(), false);
        } else {
            showFeedback(result.message(), true);
        }
    }

    // ---------------------------------------------
    // Private helpers
    // ---------------------------------------------

    private void onSelectionChanged(AdminTripDTO trip) {
        if (trip == null) {
            detailBinder.clear(TripDetailMapper.empty());
            return;
        }
        detailBinder.bind(TripDetailMapper.from(trip));
    }

    private void updateCountLabels() {
        long accepted = count(TripStatus.ACCEPTED);
        long inProgress = count(TripStatus.IN_PROGRESS);
        long completed = count(TripStatus.COMPLETED);
        long pending = count(TripStatus.PENDING);
        int total = viewModel.getAllTrips().size();
        int shown = viewModel.getFilteredTrips().size();

        if (filterAllButton != null)
            filterAllButton.setText("Todos (" + total + ")");
        if (filterActiveButton != null)
            filterActiveButton.setText("Aceites (" + accepted + ")");
        if (filterInactiveButton != null)
            filterInactiveButton.setText("Em progresso (" + inProgress + ")");
        if (filterBlockedButton != null)
            filterBlockedButton.setText("Concluidas (" + completed + ")");
        if (filterPendingButton != null)
            filterPendingButton.setText("Pendentes (" + pending + ")");
        if (listInfoLabel != null)
            listInfoLabel.setText("A mostrar " + shown + " de " + total + " viagens");
    }

    private long count(TripStatus status) {
        return viewModel.getAllTrips().stream()
                .filter(t -> t.getStatus() == status)
                .count();
    }

    private void setupColumns() {
        tripIdColumn.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getId() == null ? "-" : "#" + cd.getValue().getId()));
        tripClientColumn.setCellValueFactory(cd -> new SimpleStringProperty(
                AdminFormatUtils.fallback(cd.getValue().getClientName())));
        tripDriverColumn.setCellValueFactory(cd -> new SimpleStringProperty(
                AdminFormatUtils.fallback(cd.getValue().getDriverName())));
        tripStatusColumn.setCellValueFactory(cd -> new SimpleStringProperty(
                AdminFormatUtils.prettyTripStatus(cd.getValue().getStatus())));
        tripTypeColumn.setCellValueFactory(cd -> new SimpleStringProperty(
                AdminFormatUtils.prettyTripType(cd.getValue().getTripType())));
        tripPriceColumn.setCellValueFactory(cd -> new SimpleStringProperty(
                AdminFormatUtils.formatTripPrice(cd.getValue())));
        tripRequestedAtColumn.setCellValueFactory(cd -> {
            LocalDateTime t = cd.getValue().getRequestTime();
            return new SimpleStringProperty(t == null ? "-" : t.toString());
        });

        // Status column with CSS class per status
        tripStatusColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll(
                        "trip-completed", "trip-in-progress", "trip-accepted",
                        "trip-pending", "trip-cancelled", "trip-rejected");
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                setText(item);
                if (getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    return;
                }
                AdminTripDTO row = getTableView().getItems().get(getIndex());
                if (row.getStatus() == null) {
                    return;
                }
                switch (row.getStatus()) {
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

    private void showFeedback(String message, boolean isError) {
        if (feedbackLabel == null) {
            return;
        }
        feedbackLabel.setText(message);
        feedbackLabel.getStyleClass().removeAll("feedback-success", "feedback-error");
        feedbackLabel.getStyleClass().add(isError ? "feedback-error" : "feedback-success");
    }
}
