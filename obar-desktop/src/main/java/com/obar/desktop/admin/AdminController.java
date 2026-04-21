package com.obar.desktop.admin;

import com.obar.bll.admin.AdminService;
import com.obar.bll.auth.AuthenticatedUserDto;
import com.obar.desktop.admin.sections.AdminSectionController;
import com.obar.desktop.admin.sections.clients.ClientsController;
import com.obar.desktop.admin.sections.drivers.DriversController;
import com.obar.desktop.admin.sections.financial.FinancialController;
import com.obar.desktop.admin.sections.trips.TripsController;
import com.obar.desktop.admin.shared.AdminFormatUtils;
import com.obar.desktop.navigation.NavigationManager;
import com.obar.desktop.session.SessionManager;
import com.obar.model.enums.UserType;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Main orchestrator for the admin page.
 */
public class AdminController {

    private enum AdminSection {
        DRIVERS("Motoristas"),
        CLIENTS("Clientes"),
        TRIPS("Viagens"),
        FINANCIAL("Financeiro");

        private final String title;

        AdminSection(String title) {
            this.title = title;
        }
    }

    private final AdminService adminService;
    private AdminSection currentSection = AdminSection.DRIVERS;

    @FXML
    private Label currentSectionLabel;

    @FXML
    private Label sidebarUserInitialsLabel;

    @FXML
    private Label sidebarUserNameLabel;

    @FXML
    private Label sidebarUserRoleLabel;

    @FXML
    private Button motoristasSectionButton;

    @FXML
    private Button clientesSectionButton;

    @FXML
    private Button viagensSectionButton;

    @FXML
    private Button financeiraSectionButton;

    @FXML
    private StackPane driversSection;

    @FXML
    private StackPane clientsSection;

    @FXML
    private StackPane tripsSection;

    @FXML
    private StackPane financialSection;

    @FXML
    private VBox detailPanel;

    @FXML
    private DriversController driversSectionController;

    @FXML
    private ClientsController clientsSectionController;

    @FXML
    private TripsController tripsSectionController;

    @FXML
    private FinancialController financialSectionController;

    public AdminController(AdminService adminService) {
        if (adminService == null) {
            throw new IllegalArgumentException("AdminService must not be null.");
        }
        this.adminService = adminService;
    }

    @FXML
    public void initialize() {
        if (!SessionManager.hasRole(UserType.ADMIN)) {
            NavigationManager.navigateToDashboard();
            return;
        }

        updateSidebarUserCard();
        configureSectionControllers();
        switchSection(AdminSection.DRIVERS);
    }

    @FXML
    public void handleMotoristasSection() {
        switchSection(AdminSection.DRIVERS);
    }

    @FXML
    public void handleClientesSection() {
        switchSection(AdminSection.CLIENTS);
    }

    @FXML
    public void handleViagensSection() {
        switchSection(AdminSection.TRIPS);
    }

    @FXML
    public void handleFinanceiraSection() {
        switchSection(AdminSection.FINANCIAL);
    }

    @FXML
    public void handleBackToDashboard() {
        NavigationManager.navigateToDashboard();
    }

    @FXML
    public void handleLogout() {
        SessionManager.logout();
        NavigationManager.navigateToLogin();
    }

    @FXML
    public void handleCloseDetailPanel() {
        if (detailPanel == null) {
            return;
        }
        detailPanel.setVisible(false);
        detailPanel.setManaged(false);
    }

    @FXML
    public void handleModalCancel() {
        // Legacy modal actions kept in AdminView.fxml; section controllers own active modals.
    }

    @FXML
    public void handleModalSave() {
        // Legacy modal actions kept in AdminView.fxml; section controllers own active modals.
    }

    @FXML
    public void handleModalConfirmDelete() {
        // Legacy modal actions kept in AdminView.fxml; section controllers own active modals.
    }

    private void configureSectionControllers() {
        wireSectionController(driversSectionController);
        wireSectionController(clientsSectionController);
        wireSectionController(tripsSectionController);
        wireSectionController(financialSectionController);
    }

    private void wireSectionController(AdminSectionController controller) {
        if (controller != null) {
            controller.setAdminService(adminService);
        }
    }

    private void switchSection(AdminSection section) {
        currentSection = section;
        if (currentSectionLabel != null) {
            currentSectionLabel.setText(section.title);
        }

        setSectionVisible(driversSection, section == AdminSection.DRIVERS);
        setSectionVisible(clientsSection, section == AdminSection.CLIENTS);
        setSectionVisible(tripsSection, section == AdminSection.TRIPS);
        setSectionVisible(financialSection, section == AdminSection.FINANCIAL);

        setButtonState(motoristasSectionButton, section == AdminSection.DRIVERS, "sidebar-item", "sidebar-item-active");
        setButtonState(clientesSectionButton, section == AdminSection.CLIENTS, "sidebar-item", "sidebar-item-active");
        setButtonState(viagensSectionButton, section == AdminSection.TRIPS, "sidebar-item", "sidebar-item-active");
        setButtonState(financeiraSectionButton, section == AdminSection.FINANCIAL, "sidebar-item", "sidebar-item-active");

        AdminSectionController active = getActiveSectionController();
        if (active != null) {
            active.onSectionActivated();
        }
    }

    private AdminSectionController getActiveSectionController() {
        return switch (currentSection) {
            case DRIVERS -> driversSectionController;
            case CLIENTS -> clientsSectionController;
            case TRIPS -> tripsSectionController;
            case FINANCIAL -> financialSectionController;
        };
    }

    private void setSectionVisible(StackPane sectionNode, boolean visible) {
        if (sectionNode == null) {
            return;
        }
        sectionNode.setVisible(visible);
        sectionNode.setManaged(visible);
    }

    private void setButtonState(Button button, boolean active, String baseClass, String activeClass) {
        if (button == null) {
            return;
        }
        button.getStyleClass().removeAll(baseClass, activeClass);
        button.getStyleClass().add(active ? activeClass : baseClass);
    }

    private void updateSidebarUserCard() {
        AuthenticatedUserDto currentUser = SessionManager.getCurrentUser();
        if (sidebarUserInitialsLabel != null) {
            sidebarUserInitialsLabel.setText(AdminFormatUtils.extractInitials(currentUser.name()));
        }
        if (sidebarUserNameLabel != null) {
            sidebarUserNameLabel.setText(AdminFormatUtils.fallback(currentUser.name()));
        }
        if (sidebarUserRoleLabel != null) {
            sidebarUserRoleLabel.setText(AdminFormatUtils.prettyUserType(currentUser.type()));
        }
    }
}
