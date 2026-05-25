package com.obar.desktop.admin.sections.drivers.controller;

import com.obar.bll.admin.AdminService;
import com.obar.bll.admin.AdminUserDTO;
import com.obar.desktop.admin.sections.drivers.presenter.DriverApprovalPresenter;
import com.obar.desktop.admin.shared.AdminFormatUtils;
import com.obar.desktop.admin.shared.AdminSectionController;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Locale;

public class PendingDriversController implements AdminSectionController {
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm", Locale.ROOT);

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
    private TableView<AdminUserDTO> pendingTable;
    @FXML
    private TableColumn<AdminUserDTO, String> colId;
    @FXML
    private TableColumn<AdminUserDTO, String> colName;
    @FXML
    private TableColumn<AdminUserDTO, String> colEmail;
    @FXML
    private TableColumn<AdminUserDTO, String> colPhone;
    @FXML
    private TableColumn<AdminUserDTO, String> colLicense;
    @FXML
    private TableColumn<AdminUserDTO, String> colRegistered;
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
    private javafx.scene.control.TextArea noteField;
    @FXML
    private Button detailApproveButton;
    @FXML
    private Button detailRejectButton;

    private AdminService adminService;
    private DriverApprovalPresenter approvalPresenter;
    private final ObservableList<AdminUserDTO> allPending = FXCollections.observableArrayList();
    private final ObservableList<AdminUserDTO> filtered = FXCollections.observableArrayList();

    @Override
    public void setAdminService(AdminService adminService) {
        this.adminService = adminService;
        if (adminService != null)
            approvalPresenter = new DriverApprovalPresenter(adminService);
    }

    @Override
    public void onSectionActivated() {
        reload();
    }

    @FXML
    public void initialize() {
        setupColumns();
        pendingTable.setItems(filtered);
        searchField.textProperty().addListener((obs, prev, now) -> applyFilter(now));
        pendingTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, prev, current) -> onSelectionChanged(current));
        updateActionButtons(null);
        showDetailPanel(false);
    }

    @FXML
    public void handleApprove() {
        doApprove(pendingTable.getSelectionModel().getSelectedItem());
    }

    @FXML
    public void handleReject() {
        doReject(pendingTable.getSelectionModel().getSelectedItem());
    }

    @FXML
    public void handleDetailApprove() {
        doApprove(pendingTable.getSelectionModel().getSelectedItem());
    }

    @FXML
    public void handleDetailReject() {
        doReject(pendingTable.getSelectionModel().getSelectedItem());
    }

    @FXML
    public void handleCloseDetail() {
        pendingTable.getSelectionModel().clearSelection();
        showDetailPanel(false);
    }

    private void doApprove(AdminUserDTO driver) {
        if (approvalPresenter == null) {
            showFeedback("Serviço não disponível.", true);
            return;
        }
        String note = noteField != null ? noteField.getText() : null;
        DriverApprovalPresenter.PersistResult result = approvalPresenter.approve(driver, note);
        showFeedback(result.message(), !result.success());
        if (result.success()) {
            pendingTable.getSelectionModel().clearSelection();
            reload();
        }
    }

    private void doReject(AdminUserDTO driver) {
        if (approvalPresenter == null) {
            showFeedback("Serviço não disponível.", true);
            return;
        }
        String note = noteField != null ? noteField.getText() : null;
        DriverApprovalPresenter.PersistResult result = approvalPresenter.reject(driver, note);
        showFeedback(result.message(), !result.success());
        if (result.success()) {
            pendingTable.getSelectionModel().clearSelection();
            reload();
        }
    }

    private void reload() {
        if (adminService == null)
            return;
        List<AdminUserDTO> pending = adminService.listPendingDrivers();
        allPending.setAll(pending);
        applyFilter(searchField.getText());
    }

    private void applyFilter(String query) {
        String q = query == null ? "" : query.trim().toLowerCase(Locale.ROOT);
        if (q.isBlank()) {
            filtered.setAll(allPending);
        } else {
            filtered.setAll(allPending.stream().filter(u -> matches(u, q)).toList());
        }
        updateInfoLabel();
    }

    private boolean matches(AdminUserDTO u, String q) {
        return contains(u.getName(), q) || contains(u.getEmail(), q) || contains(u.getPhone(), q)
                || contains(u.getLicenseNumber(), q);
    }

    private boolean contains(String value, String q) {
        return value != null && value.toLowerCase(Locale.ROOT).contains(q);
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

    private void bindDetailPanel(AdminUserDTO u) {
        setLabelText(detailNameLabel, u.getName());
        setLabelText(detailEmailLabel, u.getEmail());
        setLabelText(detailPhoneLabel, u.getPhone());
        setLabelText(detailLicenseLabel, u.getLicenseNumber());
        setLabelText(detailRegisteredLabel, u.getCreatedAt() != null ? u.getCreatedAt().format(DATE_FMT) : null);
    }

    private void updateActionButtons(AdminUserDTO selected) {
        boolean has = selected != null;
        setVisibleManaged(approveButton, has);
        setVisibleManaged(rejectButton, has);
        setVisibleManaged(detailApproveButton, has);
        setVisibleManaged(detailRejectButton, has);
    }

    private void showDetailPanel(boolean visible) {
        if (detailPanel != null) {
            detailPanel.setVisible(visible);
            detailPanel.setManaged(visible);
        }
        if (!visible && noteField != null)
            noteField.clear();
    }

    private void updateInfoLabel() {
        if (listInfoLabel == null)
            return;
        int total = allPending.size();
        int shown = filtered.size();
        listInfoLabel.setText(
                total == 0 ? "Nenhuma aprovação pendente" : "A mostrar " + shown + " de " + total + " pendentes");
    }

    private void setupColumns() {
        colId.setCellValueFactory(
                cd -> new SimpleStringProperty(cd.getValue().getId() == null ? "-" : "#" + cd.getValue().getId()));
        colName.setCellValueFactory(cd -> new SimpleStringProperty(AdminFormatUtils.fallback(cd.getValue().getName())));
        colEmail.setCellValueFactory(
                cd -> new SimpleStringProperty(AdminFormatUtils.fallback(cd.getValue().getEmail())));
        colPhone.setCellValueFactory(
                cd -> new SimpleStringProperty(AdminFormatUtils.fallback(cd.getValue().getPhone())));
        colLicense.setCellValueFactory(
                cd -> new SimpleStringProperty(AdminFormatUtils.fallback(cd.getValue().getLicenseNumber())));
        colRegistered.setCellValueFactory(cd -> {
            var t = cd.getValue().getCreatedAt();
            return new SimpleStringProperty(t != null ? t.format(DATE_FMT) : "—");
        });
        colName.setCellFactory(col -> new TableCell<>() {
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
        if (feedbackLabel == null)
            return;
        feedbackLabel.setText(message);
        feedbackLabel.getStyleClass().removeAll("feedback-success", "feedback-error");
        feedbackLabel.getStyleClass().add(isError ? "feedback-error" : "feedback-success");
    }

    private void setLabelText(Label label, String value) {
        if (label != null)
            label.setText(value);
    }

    private void setVisibleManaged(Button button, boolean visible) {
        if (button == null)
            return;
        button.setVisible(visible);
        button.setManaged(visible);
    }
}
