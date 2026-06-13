package com.obar.desktop.admin.sections.drivers;

import com.obar.bll.admin.AdminService;
import com.obar.bll.admin.AdminUserDTO;
import com.obar.desktop.admin.sections.AdminSectionController;
import com.obar.desktop.admin.shared.AdminFormatUtils;
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
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Controller for the pending driver approval queue.
 *
 * <p>
 * Loads pending drivers from {@link AdminService}, applies the search filter,
 * displays the selected driver details, and sends approval or rejection actions
 * back to the service.
 * </p>
 */
public class PendingDriversController implements AdminSectionController {

    private static final DateTimeFormatter REGISTERED_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm",
            Locale.ROOT);

    @FXML
    private TextField searchField;
    @FXML
    private Label listInfoLabel;
    @FXML
    private Label feedbackLabel;
    @FXML
    private Button approveButton;
    @FXML
    private Button rejectButton;

    @FXML
    private TableView<AdminUserDTO> pendingDriversTable;
    @FXML
    private TableColumn<AdminUserDTO, String> pendingDriverIdColumn;
    @FXML
    private TableColumn<AdminUserDTO, String> pendingDriverNameColumn;
    @FXML
    private TableColumn<AdminUserDTO, String> pendingDriverEmailColumn;
    @FXML
    private TableColumn<AdminUserDTO, String> pendingDriverPhoneColumn;
    @FXML
    private TableColumn<AdminUserDTO, String> pendingDriverLicenseColumn;
    @FXML
    private TableColumn<AdminUserDTO, String> pendingDriverRegisteredColumn;

    @FXML
    private VBox detailPanel;
    @FXML
    private Label detailNameLabel;
    @FXML
    private Label detailEmailLabel;
    @FXML
    private Label detailPhoneLabel;
    @FXML
    private Label detailLicenseLabel;
    @FXML
    private Label detailRegisteredLabel;
    @FXML
    private TextArea approvalNoteField;
    @FXML
    private Button detailApproveButton;
    @FXML
    private Button detailRejectButton;

    private final ObservableList<AdminUserDTO> allPendingDrivers = FXCollections.observableArrayList();
    private final FilteredList<AdminUserDTO> filteredPendingDrivers = new FilteredList<>(allPendingDrivers,
            driver -> true);

    private AdminService adminService;

    @Override
    public void setAdminService(AdminService adminService) {
        this.adminService = adminService;
    }

    @Override
    public void onSectionActivated() {
        reload();
    }

    @FXML
    public void initialize() {
        setupColumns();
        pendingDriversTable.setItems(filteredPendingDrivers);

        searchField.textProperty().addListener((obs, oldText, newText) -> applySearchFilter());
        filteredPendingDrivers
                .addListener((javafx.collections.ListChangeListener<AdminUserDTO>) change -> updateInfoLabel());
        allPendingDrivers
                .addListener((javafx.collections.ListChangeListener<AdminUserDTO>) change -> updateInfoLabel());
        pendingDriversTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, previous, selected) -> onSelectionChanged(selected));

        updateActionButtons(null);
        showDetailPanel(false);
        updateInfoLabel();
    }

    @FXML
    public void handleApprove() {
        approveSelectedDriver();
    }

    @FXML
    public void handleReject() {
        rejectSelectedDriver();
    }

    @FXML
    public void handleCloseDetail() {
        pendingDriversTable.getSelectionModel().clearSelection();
        showDetailPanel(false);
    }

    private void approveSelectedDriver() {
        AdminUserDTO driver = pendingDriversTable.getSelectionModel().getSelectedItem();
        if (driver == null) {
            showFeedback("Selecione um motorista pendente primeiro.", true);
            return;
        }
        if (adminService == null) {
            showFeedback("Servico nao disponivel.", true);
            return;
        }

        try {
            String note = approvalNoteField == null ? null : approvalNoteField.getText();
            adminService.approveDriver(driver.id(), note);
            showFeedback("Motorista \"" + driver.name() + "\" aprovado com sucesso.", false);
            pendingDriversTable.getSelectionModel().clearSelection();
            reload();
        } catch (IllegalArgumentException exception) {
            showFeedback(exception.getMessage(), true);
        }
    }

    private void rejectSelectedDriver() {
        AdminUserDTO driver = pendingDriversTable.getSelectionModel().getSelectedItem();
        if (driver == null) {
            showFeedback("Selecione um motorista pendente primeiro.", true);
            return;
        }
        if (adminService == null) {
            showFeedback("Servico nao disponivel.", true);
            return;
        }

        try {
            String note = approvalNoteField == null ? null : approvalNoteField.getText();
            adminService.rejectDriver(driver.id(), note);
            showFeedback("Motorista \"" + driver.name() + "\" rejeitado.", false);
            pendingDriversTable.getSelectionModel().clearSelection();
            reload();
        } catch (IllegalArgumentException exception) {
            showFeedback(exception.getMessage(), true);
        }
    }

    private void reload() {
        if (adminService != null) {
            allPendingDrivers.setAll(adminService.listPendingDrivers());
            applySearchFilter();
        }
        updateInfoLabel();
    }

    private void applySearchFilter() {
        String searchText = searchField == null ? "" : searchField.getText();
        filteredPendingDrivers.setPredicate(driver -> matchesPendingDriverSearch(driver, searchText));
        updateInfoLabel();
    }

    static boolean matchesPendingDriverSearch(AdminUserDTO driver, String searchText) {
        String query = AdminFormatUtils.normalize(searchText);
        if (query.isBlank()) {
            return true;
        }
        return AdminFormatUtils.normalize(driver.name()).contains(query)
                || AdminFormatUtils.normalize(driver.email()).contains(query)
                || AdminFormatUtils.normalize(driver.phone()).contains(query)
                || AdminFormatUtils.normalize(driver.licenseNumber()).contains(query);
    }

    private void onSelectionChanged(AdminUserDTO driver) {
        updateActionButtons(driver);
        if (driver == null) {
            showDetailPanel(false);
            return;
        }
        bindDetailPanel(driver);
        showDetailPanel(true);
    }

    private void bindDetailPanel(AdminUserDTO driver) {
        setText(detailNameLabel, driver.name());
        setText(detailEmailLabel, driver.email());
        setText(detailPhoneLabel, driver.phone());
        setText(detailLicenseLabel, driver.licenseNumber());
        setText(detailRegisteredLabel,
                driver.createdAt() == null ? null : driver.createdAt().format(REGISTERED_DATE_FORMAT));
    }

    private void updateActionButtons(AdminUserDTO selected) {
        boolean hasSelection = selected != null;
        setManaged(approveButton, hasSelection);
        setManaged(rejectButton, hasSelection);
        setManaged(detailApproveButton, hasSelection);
        setManaged(detailRejectButton, hasSelection);
    }

    private void showDetailPanel(boolean visible) {
        if (detailPanel != null) {
            detailPanel.setVisible(visible);
            detailPanel.setManaged(visible);
        }
        if (!visible && approvalNoteField != null) {
            approvalNoteField.clear();
        }
    }

    private void updateInfoLabel() {
        if (listInfoLabel == null) {
            return;
        }
        int total = allPendingDrivers.size();
        int shown = filteredPendingDrivers.size();
        listInfoLabel.setText(total == 0
                ? "Nenhuma aprovacao pendente"
                : "A mostrar " + shown + " de " + total + " pendentes");
    }

    private void setupColumns() {
        pendingDriverIdColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().id() == null ? "-" : "#" + cellData.getValue().id()));
        pendingDriverNameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                AdminFormatUtils.fallback(cellData.getValue().name())));
        pendingDriverEmailColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                AdminFormatUtils.fallback(cellData.getValue().email())));
        pendingDriverPhoneColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                AdminFormatUtils.fallback(cellData.getValue().phone())));
        pendingDriverLicenseColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                AdminFormatUtils.fallback(cellData.getValue().licenseNumber())));
        pendingDriverRegisteredColumn.setCellValueFactory(cellData -> {
            var registeredAt = cellData.getValue().createdAt();
            return new SimpleStringProperty(registeredAt == null ? "-" : registeredAt.format(REGISTERED_DATE_FORMAT));
        });

        pendingDriverNameColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().remove("status-pending");
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                setText(item);
                getStyleClass().add("status-pending");
            }
        });
    }

    private void showFeedback(String message, boolean isError) {
        if (feedbackLabel == null) {
            return;
        }
        feedbackLabel.setText(message == null ? "" : message);
        feedbackLabel.getStyleClass().removeAll("feedback-success", "feedback-error");
        feedbackLabel.getStyleClass().add(isError ? "feedback-error" : "feedback-success");
    }

    private void setText(Label label, String value) {
        if (label != null) {
            label.setText(value == null || value.isBlank() ? "-" : value);
        }
    }

    private void setManaged(Button button, boolean visible) {
        if (button == null) {
            return;
        }
        button.setVisible(visible);
        button.setManaged(visible);
    }
}
