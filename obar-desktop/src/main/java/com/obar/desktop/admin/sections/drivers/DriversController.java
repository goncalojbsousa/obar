package com.obar.desktop.admin.sections.drivers;

import com.obar.bll.admin.AdminService;
import com.obar.bll.admin.AdminUserDTO;
import com.obar.desktop.admin.sections.users.AbstractUsersController;
import com.obar.desktop.admin.sections.users.UsersViewModel;
import com.obar.model.enums.AccountStatus;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

/**
 * Controller for the Drivers admin section.
 *
 * <p>Extends {@link AbstractUsersController} for all generic user-management
 * behaviour, and adds driver-specific approval/rejection actions delegated
 * to {@link DriverApprovalPresenter}.</p>
 */
public class DriversController extends AbstractUsersController {

    @FXML
    private Button approveDriverButton;
    @FXML
    private Button rejectDriverButton;
    @FXML
    private Button detailApproveButton;
    @FXML
    private Button detailRejectButton;

    private DriverApprovalPresenter approvalPresenter;

    @Override
    protected UsersViewModel createViewModel() {
        return new DriversViewModel();
    }

    @Override
    public void setAdminService(AdminService adminService) {
        super.setAdminService(adminService);
        if (adminService != null) {
            approvalPresenter = new DriverApprovalPresenter(adminService);
        }
    }

    @Override
    @FXML
    public void initialize() {
        super.initialize();
        updateApprovalButtons(null);
        // Update approval button visibility whenever selection changes
        usersTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, previous, current) -> updateApprovalButtons(current));
    }

    /**
     * Approves the currently selected pending driver.
     * Triggered by the toolbar button or the detail panel button.
     */
    @FXML
    public void handleApproveDriver() {
        if (approvalPresenter == null) {
            showFeedback("Servico nao disponivel.", true);
            return;
        }
        AdminUserDTO selected = usersTable.getSelectionModel().getSelectedItem();
        DriverApprovalPresenter.PersistResult result = approvalPresenter.approve(selected, null);
        if (result.success()) {
            reloadAndClearSelection();
        }
        showFeedback(result.message(), !result.success());
    }

    /**
     * Rejects the currently selected pending driver.
     * Triggered by the toolbar button or the detail panel button.
     */
    @FXML
    public void handleRejectDriver() {
        if (approvalPresenter == null) {
            showFeedback("Servico nao disponivel.", true);
            return;
        }
        AdminUserDTO selected = usersTable.getSelectionModel().getSelectedItem();
        DriverApprovalPresenter.PersistResult result = approvalPresenter.reject(selected, null);
        if (result.success()) {
            reloadAndClearSelection();
        }
        showFeedback(result.message(), !result.success());
    }

    // -------------------------------------------------------
    // Private helpers
    // -------------------------------------------------------

    private void reloadAndClearSelection() {
        usersTable.getSelectionModel().clearSelection();
        onSectionActivated();
    }

    /**
     * Shows the approve/reject buttons only when a PENDING driver is selected.
     */
    private void updateApprovalButtons(AdminUserDTO selected) {
        boolean isPending = selected != null && selected.getStatus() == AccountStatus.PENDING;

        if (approveDriverButton != null) approveDriverButton.setVisible(isPending);
        if (approveDriverButton != null) approveDriverButton.setManaged(isPending);
        if (rejectDriverButton != null) rejectDriverButton.setVisible(isPending);
        if (rejectDriverButton != null) rejectDriverButton.setManaged(isPending);
        if (detailApproveButton != null) detailApproveButton.setVisible(isPending);
        if (detailApproveButton != null) detailApproveButton.setManaged(isPending);
        if (detailRejectButton != null) detailRejectButton.setVisible(isPending);
        if (detailRejectButton != null) detailRejectButton.setManaged(isPending);
    }

    // Expose showFeedback to this subclass (it's private in the parent, delegate via helper)
    private void showFeedback(String message, boolean isError) {
        if (feedbackLabel == null) return;
        feedbackLabel.setText(message);
        feedbackLabel.getStyleClass().removeAll("feedback-success", "feedback-error");
        feedbackLabel.getStyleClass().add(isError ? "feedback-error" : "feedback-success");
    }
}
