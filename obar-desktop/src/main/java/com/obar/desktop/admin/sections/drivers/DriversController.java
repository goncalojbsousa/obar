package com.obar.desktop.admin.sections.drivers;

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
    @FXML
    public void initialize() {
        super.initialize();
        updateApprovalButtons(null);
        usersTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, previous, current) -> updateApprovalButtons(current));
    }

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
            getAdminService().approveDriver(selected.id(), null);
            reloadAndClearSelection();
            showFeedback("Motorista \"" + selected.name() + "\" aprovado com sucesso.", false);
        } catch (IllegalArgumentException exception) {
            showFeedback(exception.getMessage(), true);
        }
    }

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
            getAdminService().rejectDriver(selected.id(), null);
            reloadAndClearSelection();
            showFeedback("Motorista \"" + selected.name() + "\" rejeitado.", false);
        } catch (IllegalArgumentException exception) {
            showFeedback(exception.getMessage(), true);
        }
    }

    private void reloadAndClearSelection() {
        usersTable.getSelectionModel().clearSelection();
        onSectionActivated();
    }

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
        return selected != null && selected.status() == AccountStatus.PENDING;
    }
}
