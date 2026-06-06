package com.obar.desktop.admin.sections.vehicles;

import com.obar.bll.admin.AdminService;
import com.obar.bll.admin.AdminVehicleCommand;
import com.obar.bll.admin.AdminVehicleDTO;
import com.obar.desktop.admin.sections.AdminSectionController;
import com.obar.desktop.admin.shared.AdminFormatUtils;
import com.obar.desktop.admin.shared.AdminModalIncludeController;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * Controller for the admin vehicle management section.
 */
public class VehiclesController implements AdminSectionController {

    private enum ModalMode {
        NONE, CREATE, EDIT, DEACTIVATE_CONFIRM, REACTIVATE_CONFIRM, DELETE_CONFIRM
    }

    private static final ObservableList<String> VEHICLE_CATEGORIES = FXCollections.observableArrayList(
            "STANDARD",
            "XL",
            "PREMIUM");

    @FXML
    private TextField searchField;
    @FXML
    private TableView<AdminVehicleDTO> vehiclesTable;
    @FXML
    private TableColumn<AdminVehicleDTO, String> vehicleIdColumn;
    @FXML
    private TableColumn<AdminVehicleDTO, String> vehicleColumn;
    @FXML
    private TableColumn<AdminVehicleDTO, String> licensePlateColumn;
    @FXML
    private TableColumn<AdminVehicleDTO, String> driverColumn;
    @FXML
    private TableColumn<AdminVehicleDTO, String> categoryColumn;
    @FXML
    private TableColumn<AdminVehicleDTO, String> yearColumn;
    @FXML
    private TableColumn<AdminVehicleDTO, String> activeColumn;
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
    private Button deactivateVehicleButton;
    @FXML
    private Button reactivateVehicleButton;

    @FXML
    private VBox detailPanel;
    @FXML
    private Label detailTitleLabel;
    @FXML
    private Label detailVehicleLabel;
    @FXML
    private Label detailLicensePlateLabel;
    @FXML
    private Label detailStatusLabel;
    @FXML
    private Label detailDriverLabel;
    @FXML
    private Label detailCategoryLabel;
    @FXML
    private Label detailYearLabel;
    @FXML
    private Label detailColorLabel;
    @FXML
    private Label detailBaseFareLabel;
    @FXML
    private Label detailPricePerKmLabel;
    @FXML
    private Button detailDeactivateVehicleButton;
    @FXML
    private Button detailReactivateVehicleButton;

    @FXML
    private StackPane vehicleModalOverlay;
    @FXML
    private Label vehicleModalTitleLabel;
    @FXML
    private Label vehicleModalErrorLabel;
    @FXML
    private VBox vehicleFormSection;
    @FXML
    private VBox vehicleConfirmSection;
    @FXML
    private Label vehicleConfirmMessageLabel;
    @FXML
    private TextField driverIdField;
    @FXML
    private TextField brandField;
    @FXML
    private TextField modelField;
    @FXML
    private TextField colorField;
    @FXML
    private TextField licensePlateField;
    @FXML
    private TextField yearField;
    @FXML
    private ComboBox<String> categoryComboBox;
    @FXML
    private TextField baseFareField;
    @FXML
    private TextField pricePerKmField;
    @FXML
    private CheckBox activeCheckBox;
    @FXML
    private Button modalSaveButton;
    @FXML
    private Button modalDeactivateButton;

    private final ObservableList<AdminVehicleDTO> allVehicles = FXCollections.observableArrayList();
    private final FilteredList<AdminVehicleDTO> filteredVehicles = new FilteredList<>(allVehicles, vehicle -> true);

    private AdminService adminService;
    private Boolean activeFilter;
    private ModalMode modalMode = ModalMode.NONE;
    private AdminVehicleDTO modalTarget;

    @Override
    public void setAdminService(AdminService adminService) {
        this.adminService = adminService;
    }

    @Override
    public void onSectionActivated() {
        reloadVehicles();
    }

    @FXML
    public void initialize() {
        configureTableColumns();
        vehiclesTable.setItems(filteredVehicles);

        searchField.textProperty().addListener((obs, oldText, newText) -> applyVehicleFilter());
        vehiclesTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, previous, selected) -> showVehicleDetails(selected));
        filteredVehicles.addListener((javafx.collections.ListChangeListener<AdminVehicleDTO>) change -> updateCountLabels());

        setDetailVisible(false);
        setModalVisible(false);
        updateVehicleActionButtons(null);
        categoryComboBox.setItems(VEHICLE_CATEGORIES);
        updateFilterChipStyles();
        updateCountLabels();
    }

    @FXML
    public void handleFilterAll() {
        setActiveFilter(null);
    }

    @FXML
    public void handleFilterActive() {
        setActiveFilter(true);
    }

    @FXML
    public void handleFilterInactive() {
        setActiveFilter(false);
    }

    @FXML
    public void handleAddVehicle() {
        modalMode = ModalMode.CREATE;
        modalTarget = null;
        vehicleModalTitleLabel.setText("Novo veiculo");
        showOnlyVehicleForm();
        clearVehicleForm();
        setModalVisible(true);
    }

    @FXML
    public void handleEditVehicle() {
        AdminVehicleDTO selectedVehicle = getSelectedVehicle();
        if (selectedVehicle == null) {
            showFeedback("Selecione um veiculo primeiro.", true);
            return;
        }

        modalMode = ModalMode.EDIT;
        modalTarget = selectedVehicle;
        vehicleModalTitleLabel.setText("Editar veiculo");
        showOnlyVehicleForm();
        fillVehicleForm(selectedVehicle);
        setModalVisible(true);
    }

    @FXML
    public void handleDeactivateVehicle() {
        AdminVehicleDTO selectedVehicle = getSelectedVehicle();
        if (selectedVehicle == null) {
            showFeedback("Selecione um veiculo primeiro.", true);
            return;
        }

        modalMode = ModalMode.DEACTIVATE_CONFIRM;
        modalTarget = selectedVehicle;
        vehicleModalTitleLabel.setText("Desativar veiculo");
        vehicleConfirmMessageLabel.setText("Desativar " + selectedVehicle.getVehicleName()
                + " (" + AdminFormatUtils.fallback(selectedVehicle.getLicensePlate()) + ")?");
        showOnlyConfirmAction(modalDeactivateButton, "Confirmar");
        setModalVisible(true);
    }

    @FXML
    public void handleReactivateVehicle() {
        AdminVehicleDTO selectedVehicle = getSelectedVehicle();
        if (selectedVehicle == null) {
            showFeedback("Selecione um veiculo primeiro.", true);
            return;
        }

        modalMode = ModalMode.REACTIVATE_CONFIRM;
        modalTarget = selectedVehicle;
        vehicleModalTitleLabel.setText("Ativar veiculo");
        vehicleConfirmMessageLabel.setText("Voltar a ativar " + selectedVehicle.getVehicleName()
                + " (" + AdminFormatUtils.fallback(selectedVehicle.getLicensePlate()) + ")?");
        showOnlyConfirmAction(modalDeactivateButton, "Confirmar");
        setModalVisible(true);
    }

    @FXML
    public void handleDeleteVehicle() {
        AdminVehicleDTO selectedVehicle = getSelectedVehicle();
        if (selectedVehicle == null) {
            showFeedback("Selecione um veiculo primeiro.", true);
            return;
        }

        modalMode = ModalMode.DELETE_CONFIRM;
        modalTarget = selectedVehicle;
        vehicleModalTitleLabel.setText("Apagar veiculo");
        vehicleConfirmMessageLabel.setText("Apagar definitivamente " + selectedVehicle.getVehicleName()
                + " (" + AdminFormatUtils.fallback(selectedVehicle.getLicensePlate()) + ")?");
        showOnlyConfirmAction(modalDeactivateButton, "Apagar");
        setModalVisible(true);
    }

    @FXML
    public void handleCloseDetailPanel() {
        vehiclesTable.getSelectionModel().clearSelection();
        setDetailVisible(false);
    }

    @FXML
    public void handleModalCancel() {
        closeModal();
    }

    @FXML
    public void handleModalSave() {
        if (modalMode == ModalMode.DEACTIVATE_CONFIRM
                || modalMode == ModalMode.REACTIVATE_CONFIRM
                || modalMode == ModalMode.DELETE_CONFIRM) {
            return;
        }

        try {
            AdminVehicleCommand command = readVehicleCommand();
            if (modalMode == ModalMode.EDIT) {
                if (modalTarget == null || modalTarget.getId() == null) {
                    showModalError("Veiculo invalido.");
                    return;
                }
                adminService.updateVehicle(modalTarget.getId(), command);
                finishModalWithSuccess("Veiculo atualizado com sucesso.");
                return;
            }

            adminService.createVehicle(command);
            finishModalWithSuccess("Veiculo criado com sucesso.");
        } catch (Exception exception) {
            showModalError("Falha ao guardar veiculo: " + exception.getMessage());
        }
    }

    @FXML
    public void handleModalDeactivate() {
        if (modalTarget == null || modalTarget.getId() == null) {
            showModalError("Veiculo invalido.");
            return;
        }

        try {
            if (modalMode == ModalMode.REACTIVATE_CONFIRM) {
                adminService.reactivateVehicle(modalTarget.getId());
                finishModalWithSuccess("Veiculo ativado com sucesso.");
                return;
            }
            if (modalMode == ModalMode.DELETE_CONFIRM) {
                adminService.deleteVehicle(modalTarget.getId());
                finishModalWithSuccess("Veiculo apagado com sucesso.");
                return;
            }

            adminService.deactivateVehicle(modalTarget.getId());
            finishModalWithSuccess("Veiculo desativado com sucesso.");
        } catch (Exception exception) {
            showModalError("Falha ao atualizar veiculo: " + exception.getMessage());
        }
    }

    private void reloadVehicles() {
        if (adminService == null) {
            return;
        }
        allVehicles.setAll(adminService.listVehicles());
        applyVehicleFilter();
        updateCountLabels();
    }

    private void configureTableColumns() {
        vehicleIdColumn.setCellValueFactory(data -> text(data.getValue().getId()));
        vehicleColumn.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getVehicleName()));
        licensePlateColumn.setCellValueFactory(data -> text(data.getValue().getLicensePlate()));
        driverColumn.setCellValueFactory(data -> new SimpleStringProperty(formatDriver(data.getValue())));
        categoryColumn.setCellValueFactory(data -> text(data.getValue().getCategory()));
        yearColumn.setCellValueFactory(data -> text(data.getValue().getYear()));
        activeColumn.setCellValueFactory(data -> new SimpleStringProperty(isActive(data.getValue()) ? "Ativo" : "Inativo"));
        activeColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty ? null : item);
                getStyleClass().removeAll("status-active", "status-inactive");
                if (!empty) {
                    getStyleClass().add("Ativo".equals(item) ? "status-active" : "status-inactive");
                }
            }
        });
    }

    private void applyVehicleFilter() {
        String query = AdminFormatUtils.normalize(searchField == null ? "" : searchField.getText());
        filteredVehicles.setPredicate(vehicle -> matchesActiveFilter(vehicle) && matchesSearch(vehicle, query));
    }

    private boolean matchesActiveFilter(AdminVehicleDTO vehicle) {
        return activeFilter == null || isActive(vehicle) == activeFilter;
    }

    private boolean matchesSearch(AdminVehicleDTO vehicle, String query) {
        if (query.isBlank()) {
            return true;
        }
        return AdminFormatUtils.normalize(vehicle.getVehicleName()).contains(query)
                || AdminFormatUtils.normalize(vehicle.getLicensePlate()).contains(query)
                || AdminFormatUtils.normalize(vehicle.getDriverName()).contains(query)
                || AdminFormatUtils.normalize(vehicle.getCategory()).contains(query)
                || AdminFormatUtils.normalize(vehicle.getColor()).contains(query)
                || AdminFormatUtils.normalize(String.valueOf(vehicle.getDriverId())).contains(query);
    }

    private void showVehicleDetails(AdminVehicleDTO vehicle) {
        if (vehicle == null) {
            setDetailVisible(false);
            return;
        }

        detailTitleLabel.setText("Detalhe do veiculo");
        detailVehicleLabel.setText(vehicle.getVehicleName());
        detailLicensePlateLabel.setText(AdminFormatUtils.fallback(vehicle.getLicensePlate()));
        detailStatusLabel.setText(isActive(vehicle) ? "Ativo" : "Inativo");
        detailDriverLabel.setText(formatDriver(vehicle));
        detailCategoryLabel.setText(AdminFormatUtils.fallback(vehicle.getCategory()));
        detailYearLabel.setText(formatInteger(vehicle.getYear()));
        detailColorLabel.setText(AdminFormatUtils.fallback(vehicle.getColor()));
        detailBaseFareLabel.setText(formatMoney(vehicle.getBaseFare()));
        detailPricePerKmLabel.setText(formatMoney(vehicle.getPricePerKm()));
        updateVehicleActionButtons(vehicle);
        setDetailVisible(true);
    }

    private void clearVehicleForm() {
        driverIdField.clear();
        brandField.clear();
        modelField.clear();
        colorField.clear();
        licensePlateField.clear();
        yearField.clear();
        categoryComboBox.getSelectionModel().select("STANDARD");
        baseFareField.clear();
        pricePerKmField.clear();
        activeCheckBox.setSelected(true);
        clearModalError();
    }

    private void fillVehicleForm(AdminVehicleDTO vehicle) {
        driverIdField.setText(formatInteger(vehicle.getDriverId()));
        brandField.setText(blankIfMissing(vehicle.getBrand()));
        modelField.setText(blankIfMissing(vehicle.getModel()));
        colorField.setText(blankIfMissing(vehicle.getColor()));
        licensePlateField.setText(blankIfMissing(vehicle.getLicensePlate()));
        yearField.setText(vehicle.getYear() == null ? "" : vehicle.getYear().toString());
        categoryComboBox.getSelectionModel().select(blankIfMissing(vehicle.getCategory()));
        baseFareField.setText(vehicle.getBaseFare() == null ? "" : vehicle.getBaseFare().toString());
        pricePerKmField.setText(vehicle.getPricePerKm() == null ? "" : vehicle.getPricePerKm().toString());
        activeCheckBox.setSelected(isActive(vehicle));
        clearModalError();
    }

    private AdminVehicleCommand readVehicleCommand() {
        return new AdminVehicleCommand(
                parseInteger(driverIdField.getText(), "Motorista"),
                brandField.getText(),
                modelField.getText(),
                colorField.getText(),
                licensePlateField.getText(),
                parseOptionalInteger(yearField.getText(), "Ano"),
                categoryComboBox.getValue(),
                parseOptionalMoney(baseFareField.getText(), "Tarifa base"),
                parseOptionalMoney(pricePerKmField.getText(), "Preco por km"),
                activeCheckBox.isSelected());
    }

    private void showOnlyVehicleForm() {
        AdminModalIncludeController.setVisible(vehicleFormSection, true);
        AdminModalIncludeController.setVisible(vehicleConfirmSection, false);
        AdminModalIncludeController.setVisible(modalSaveButton, true);
        AdminModalIncludeController.setVisible(modalDeactivateButton, false);
    }

    private void showOnlyConfirmAction(Button actionButton, String actionText) {
        AdminModalIncludeController.setVisible(vehicleFormSection, false);
        AdminModalIncludeController.setVisible(vehicleConfirmSection, true);
        AdminModalIncludeController.setVisible(modalSaveButton, false);
        AdminModalIncludeController.setVisible(actionButton, true);
        actionButton.setText(actionText);
        clearModalError();
    }

    private void finishModalWithSuccess(String message) {
        closeModal();
        reloadVehicles();
        showFeedback(message, false);
    }

    private void closeModal() {
        modalMode = ModalMode.NONE;
        modalTarget = null;
        setModalVisible(false);
        clearModalError();
    }

    private AdminVehicleDTO getSelectedVehicle() {
        return vehiclesTable == null ? null : vehiclesTable.getSelectionModel().getSelectedItem();
    }

    private void setActiveFilter(Boolean activeFilter) {
        this.activeFilter = activeFilter;
        applyVehicleFilter();
        updateFilterChipStyles();
        updateCountLabels();
    }

    private void updateCountLabels() {
        int total = allVehicles.size();
        int shown = filteredVehicles.size();
        setButtonText(filterAllButton, "Todos (" + total + ")");
        setButtonText(filterActiveButton, "Ativos (" + countByActive(true) + ")");
        setButtonText(filterInactiveButton, "Inativos (" + countByActive(false) + ")");
        listInfoLabel.setText("A mostrar " + shown + " de " + total + " veiculos");
    }

    private void updateVehicleActionButtons(AdminVehicleDTO vehicle) {
        boolean hasVehicle = vehicle != null;
        boolean vehicleIsActive = isActive(vehicle);

        setVisibleManaged(deactivateVehicleButton, hasVehicle && vehicleIsActive);
        setVisibleManaged(detailDeactivateVehicleButton, hasVehicle && vehicleIsActive);
        setVisibleManaged(reactivateVehicleButton, hasVehicle && !vehicleIsActive);
        setVisibleManaged(detailReactivateVehicleButton, hasVehicle && !vehicleIsActive);
    }

    private long countByActive(boolean active) {
        return allVehicles.stream().filter(vehicle -> isActive(vehicle) == active).count();
    }

    private void updateFilterChipStyles() {
        updateChip(filterAllButton, activeFilter == null);
        updateChip(filterActiveButton, Boolean.TRUE.equals(activeFilter));
        updateChip(filterInactiveButton, Boolean.FALSE.equals(activeFilter));
    }

    private void updateChip(Button button, boolean active) {
        button.getStyleClass().removeAll("filter-chip", "filter-chip-active");
        button.getStyleClass().add(active ? "filter-chip-active" : "filter-chip");
    }

    private void showFeedback(String message, boolean isError) {
        feedbackLabel.setText(message == null ? "" : message);
        feedbackLabel.getStyleClass().removeAll("feedback-success", "feedback-error");
        feedbackLabel.getStyleClass().add(isError ? "feedback-error" : "feedback-success");
    }

    private void showModalError(String message) {
        vehicleModalErrorLabel.setText(message == null ? "" : message);
        AdminModalIncludeController.setVisible(vehicleModalErrorLabel, true);
    }

    private void clearModalError() {
        vehicleModalErrorLabel.setText("");
        AdminModalIncludeController.setVisible(vehicleModalErrorLabel, false);
    }

    private void setModalVisible(boolean visible) {
        AdminModalIncludeController.setVisible(vehicleModalOverlay, visible);
    }

    private void setDetailVisible(boolean visible) {
        AdminModalIncludeController.setVisible(detailPanel, visible);
        if (!visible) {
            updateVehicleActionButtons(null);
        }
    }

    private static SimpleStringProperty text(Object value) {
        return new SimpleStringProperty(value == null ? "-" : value.toString());
    }

    private static String formatDriver(AdminVehicleDTO vehicle) {
        String driverName = AdminFormatUtils.fallback(vehicle.getDriverName());
        return vehicle.getDriverId() == null ? driverName : "#" + vehicle.getDriverId() + " | " + driverName;
    }

    private static String formatInteger(Integer value) {
        return value == null ? "-" : value.toString();
    }

    private static String formatMoney(BigDecimal value) {
        return value == null ? "-" : "EUR " + value;
    }

    private static String blankIfMissing(String value) {
        return value == null || value.isBlank() ? "" : value;
    }

    private static boolean isActive(AdminVehicleDTO vehicle) {
        return vehicle != null && !Boolean.FALSE.equals(vehicle.getActive());
    }

    private static void setButtonText(Button button, String text) {
        if (button != null) {
            button.setText(text);
        }
    }

    private static void setVisibleManaged(Button button, boolean visible) {
        if (button == null) {
            return;
        }
        button.setVisible(visible);
        button.setManaged(visible);
    }

    private static Integer parseInteger(String value, String fieldName) {
        String safeValue = value == null ? "" : value.trim();
        if (safeValue.isBlank()) {
            throw new IllegalArgumentException(fieldName + " e obrigatorio.");
        }
        return parseOptionalInteger(safeValue, fieldName);
    }

    private static Integer parseOptionalInteger(String value, String fieldName) {
        String safeValue = value == null ? "" : value.trim();
        if (safeValue.isBlank()) {
            return null;
        }
        try {
            return Integer.valueOf(safeValue);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(fieldName + " deve ser um numero inteiro.");
        }
    }

    private static BigDecimal parseOptionalMoney(String value, String fieldName) {
        String safeValue = value == null ? "" : value.trim().replace(",", ".");
        if (safeValue.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(safeValue);
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException(fieldName + " deve ser um valor numerico.");
        }
    }
}
