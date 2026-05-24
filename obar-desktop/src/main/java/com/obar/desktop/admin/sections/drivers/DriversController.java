package com.obar.desktop.admin.sections.drivers;

import com.obar.bll.admin.AdminService;
import com.obar.bll.admin.AdminUserDTO;
import com.obar.desktop.admin.sections.users.UsersController;
import com.obar.model.enums.AccountStatus;
import com.obar.model.enums.UserType;
import javafx.fxml.FXML;
import javafx.scene.control.Button;

/**
 * Controller for the Drivers admin section.
 *
 * <p>
 * Provides driver-specific labels and handles approval actions for pending
 * driver accounts.
 * </p>
 */
public class DriversController extends UsersController {

    private static final UserSectionConfig CONFIG = new UserSectionConfig(
            UserType.DRIVER,
            "Motoristas",
            "+ Novo Motorista",
            "Motorista",
            "Pesquisar por nome, email, telefone, licenca...",
            "Licenca",
            "Numero de licenca",
            "Avaliacao",
            "Viagens",
            "Avaliacao",
            "Viagens",
            "Disponivel",
            "Estado");

    @FXML
    private Button approveDriverButton;
    @FXML
    private Button rejectDriverButton;
    @FXML
    private Button detailApproveButton;
    @FXML
    private Button detailRejectButton;

    @Override
    protected UserSectionConfig sectionConfig() {
        return CONFIG;
    }

    @Override
    public void setAdminService(AdminService adminService) {
        super.setAdminService(adminService);
    }

    @Override
    @FXML
    public void initialize() {
        super.initialize();
        updateApprovalButtons(null);
        usersTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, previous, current) -> updateApprovalButtons(current));
    }

    /**
     * Approves the currently selected pending driver.
     * Triggered by the toolbar button or the detail panel button.
     */
    @FXML
    public void handleApproveDriver() {
        if (getAdminService() == null) {
            showFeedback("Servico nao disponivel.", true);
            return;
        }
        AdminUserDTO selected = getSelectedUser();
        if (!canApproveOrReject(selected)) {
            showFeedback("Selecione um motorista pendente primeiro.", true);
            return;
        }
        try {
            getAdminService().approveDriver(selected.getId(), null);
            reloadAndClearSelection();
            showFeedback("Motorista \"" + selected.getName() + "\" aprovado com sucesso.", false);
        } catch (IllegalArgumentException exception) {
            showFeedback(exception.getMessage(), true);
        }
    }

    /**
     * Rejects the currently selected pending driver.
     * Triggered by the toolbar button or the detail panel button.
     */
    @FXML
    public void handleRejectDriver() {
        if (getAdminService() == null) {
            showFeedback("Servico nao disponivel.", true);
            return;
        }
        AdminUserDTO selected = getSelectedUser();
        if (!canApproveOrReject(selected)) {
            showFeedback("Selecione um motorista pendente primeiro.", true);
            return;
        }
        try {
            getAdminService().rejectDriver(selected.getId(), null);
            reloadAndClearSelection();
            showFeedback("Motorista \"" + selected.getName() + "\" rejeitado.", false);
        } catch (IllegalArgumentException exception) {
            showFeedback(exception.getMessage(), true);
        }
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
        boolean isPending = canApproveOrReject(selected);

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

    private boolean canApproveOrReject(AdminUserDTO selected) {
        return selected != null && selected.getStatus() == AccountStatus.PENDING;
    }
}
