package com.obar.desktop.admin.sections.drivers.controller;

import com.obar.bll.admin.AdminService;
import com.obar.bll.admin.AdminUserDTO;
import com.obar.desktop.admin.sections.drivers.presenter.DriverApprovalPresenter;
import com.obar.desktop.admin.sections.drivers.viewmodel.DriversViewModel;
import com.obar.desktop.admin.sections.users.controller.AbstractUsersController;
import com.obar.model.enums.AccountStatus;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

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
    protected DriversViewModel createViewModel() {
        return new DriversViewModel();
    }

    @Override
    public void setAdminService(AdminService adminService) {
        super.setAdminService(adminService);
        if (adminService != null)
            approvalPresenter = new DriverApprovalPresenter(adminService);
    }

    @Override
    @FXML
    public void initialize() {
        super.initialize();
        updateApprovalButtons(null);
        usersTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, previous, current) -> updateApprovalButtons(current));
    }

    @FXML
    public void handleApproveDriver() {
        if (approvalPresenter == null) {
            showFeedback("Servico nao disponivel.", true);
            return;
        }
        AdminUserDTO selected = usersTable.getSelectionModel().getSelectedItem();
        DriverApprovalPresenter.PersistResult result = approvalPresenter.approve(selected, null);
        if (result.success())
            reloadAndClearSelection();
        showFeedback(result.message(), !result.success());
    }

    @FXML
    public void handleRejectDriver() {
        if (approvalPresenter == null) {
            showFeedback("Servico nao disponivel.", true);
            return;
        }
        AdminUserDTO selected = usersTable.getSelectionModel().getSelectedItem();
        DriverApprovalPresenter.PersistResult result = approvalPresenter.reject(selected, null);
        if (result.success())
            reloadAndClearSelection();
        showFeedback(result.message(), !result.success());
    }

    private void reloadAndClearSelection() {
        usersTable.getSelectionModel().clearSelection();
        onSectionActivated();
    }

    private void updateApprovalButtons(AdminUserDTO selected) {
        boolean isPending = selected != null && selected.getStatus() == AccountStatus.PENDING;
        if (approveDriverButton != null)
            approveDriverButton.setVisible(isPending);
        if (approveDriverButton != null)
            approveDriverButton.setManaged(isPending);
        if (rejectDriverButton != null)
            rejectDriverButton.setVisible(isPending);
        if (rejectDriverButton != null)
            rejectDriverButton.setManaged(isPending);
        if (detailApproveButton != null)
            detailApproveButton.setVisible(isPending);
        if (detailApproveButton != null)
            detailApproveButton.setManaged(isPending);
        if (detailRejectButton != null)
            detailRejectButton.setVisible(isPending);
        if (detailRejectButton != null)
            detailRejectButton.setManaged(isPending);
    }
}
