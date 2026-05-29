package com.obar.desktop.admin;

import com.obar.bll.admin.AdminService;
import com.obar.bll.auth.AuthenticatedUserDto;
import com.obar.desktop.admin.sections.AdminSectionController;
import com.obar.desktop.admin.sections.clients.ClientsController;
import com.obar.desktop.admin.sections.drivers.DriversController;
import com.obar.desktop.admin.sections.drivers.PendingDriversController;
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

/**
 * Main orchestrator for the admin page.
 */
public class AdminController {

    private enum AdminSection {
        DRIVERS("Motoristas"),
        PENDING_DRIVERS("Aprovações Pendentes"),
        CLIENTS("Clientes"),
        TRIPS("Viagens"),
        FINANCIAL("Financeiro");

        private final String title;

        AdminSection(String title) {
            this.title = title;
        }
    }

    private final AdminService adminService;
    private AdminSection currentSection;

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
    private Button pendingDriversSectionButton;
    @FXML
    private Button clientesSectionButton;
    @FXML
    private Button viagensSectionButton;
    @FXML
    private Button financeiraSectionButton;

    @FXML
    private StackPane driversSection;
    @FXML
    private javafx.scene.layout.HBox pendingDriversSection;
    @FXML
    private StackPane clientsSection;
    @FXML
    private StackPane tripsSection;
    @FXML
    private StackPane financialSection;

    @FXML
    private DriversController driversSectionController;
    @FXML
    private PendingDriversController pendingDriversSectionController;
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
            NavigationManager.navigateToLogin();
            return;
        }

        updateSidebarUserCard();
        configureSectionControllers();
        switchSection(AdminSection.DRIVERS);
    }

    // ── Handlers da sidebar ───────────────────────────────────────────────────

    @FXML
    public void handleMotoristasSection() {
        switchSection(AdminSection.DRIVERS);
    }

    @FXML
    public void handlePendingDriversSection() {
        switchSection(AdminSection.PENDING_DRIVERS);
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
    public void handleLogout() {
        SessionManager.logout();
        NavigationManager.navigateToLogin();
    }

    @FXML
    public void handleModalCancel() {
        /* delegado */ }

    @FXML
    public void handleModalSave() {
        /* delegado */ }

    @FXML
    public void handleModalConfirmDelete() {
        /* delegado */ }

    // ── Private ───────────────────────────────────────────────────────────────

    private void configureSectionControllers() {
        wireSectionController(driversSectionController);
        wireSectionController(pendingDriversSectionController);
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
        setSectionVisible(pendingDriversSection, section == AdminSection.PENDING_DRIVERS);
        setSectionVisible(clientsSection, section == AdminSection.CLIENTS);
        setSectionVisible(tripsSection, section == AdminSection.TRIPS);
        setSectionVisible(financialSection, section == AdminSection.FINANCIAL);

        setButtonState(motoristasSectionButton, section == AdminSection.DRIVERS);
        setButtonState(pendingDriversSectionButton, section == AdminSection.PENDING_DRIVERS);
        setButtonState(clientesSectionButton, section == AdminSection.CLIENTS);
        setButtonState(viagensSectionButton, section == AdminSection.TRIPS);
        setButtonState(financeiraSectionButton, section == AdminSection.FINANCIAL);

        AdminSectionController active = getActiveSectionController();
        if (active != null) {
            active.onSectionActivated();
        }
    }

    private AdminSectionController getActiveSectionController() {
        return switch (currentSection) {
            case DRIVERS -> driversSectionController;
            case PENDING_DRIVERS -> pendingDriversSectionController;
            case CLIENTS -> clientsSectionController;
            case TRIPS -> tripsSectionController;
            case FINANCIAL -> financialSectionController;
        };
    }

    private void setSectionVisible(javafx.scene.Node node, boolean visible) {
        if (node == null)
            return;
        node.setVisible(visible);
        node.setManaged(visible);
    }

    private void setButtonState(Button button, boolean active) {
        if (button == null)
            return;
        button.getStyleClass().removeAll("sidebar-item", "sidebar-item-active", "pending-sidebar-item",
                "pending-sidebar-item-active");
        button.getStyleClass().add(active ? "sidebar-item-active" : "sidebar-item");
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
