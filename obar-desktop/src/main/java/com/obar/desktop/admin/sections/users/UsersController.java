package com.obar.desktop.admin.sections.users;

import static com.obar.desktop.admin.shared.AdminPdfExportService.column;

import com.obar.bll.admin.AdminService;
import com.obar.bll.admin.AdminUserCommand;
import com.obar.bll.admin.AdminUserDTO;
import com.obar.desktop.admin.sections.AdminSectionController;
import com.obar.desktop.admin.shared.AdminFormatUtils;
import com.obar.desktop.admin.shared.AdminModalIncludeController;
import com.obar.desktop.admin.shared.AdminPdfExportService;
import com.obar.model.enums.AccountStatus;
import com.obar.model.enums.UserType;
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
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;

/**
 * Shared controller for the users screens: drivers and clients.
 *
 * <p>
 * The FXML files define the view. The DTOs and {@link AdminService} provide the
 * model and business operations. This controller manages table filtering, modal
 * forms, detail panels, and service calls for user records.
 * </p>
 */
public abstract class UsersController implements AdminSectionController {

    private static final DateTimeFormatter EXPORT_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm",
            Locale.forLanguageTag("pt-PT"));

    public record UserSectionConfig(
            UserType type,
            String sectionTitle,
            String createLabel,
            String badgeLabel,
            String searchPrompt,
            String referenceTitle,
            String referencePrompt,
            String metricTitle,
            String volumeTitle,
            String detailCardOneTitle,
            String detailCardTwoTitle,
            String detailCardThreeTitle,
            String detailCardFourTitle) {
    }

    private enum ModalMode {
        NONE, CREATE, EDIT, BLOCK_CONFIRM, UNBLOCK_CONFIRM, DELETE_CONFIRM
    }

    @FXML
    protected VBox root;
    @FXML
    protected TextField searchField;
    @FXML
    protected TableView<AdminUserDTO> usersTable;
    @FXML
    protected TableColumn<AdminUserDTO, String> nameColumn;
    @FXML
    protected TableColumn<AdminUserDTO, String> userIdColumn;
    @FXML
    protected TableColumn<AdminUserDTO, String> emailColumn;
    @FXML
    protected TableColumn<AdminUserDTO, String> statusColumn;
    @FXML
    protected TableColumn<AdminUserDTO, String> metricColumn;
    @FXML
    protected TableColumn<AdminUserDTO, String> volumeColumn;
    @FXML
    protected TableColumn<AdminUserDTO, String> referenceColumn;
    @FXML
    protected TableColumn<AdminUserDTO, String> createdAtColumn;
    @FXML
    protected Label listInfoLabel;
    @FXML
    protected Label feedbackLabel;
    @FXML
    protected Button addUserButton;
    @FXML
    protected Button blockUserButton;
    @FXML
    protected Button filterAllButton;
    @FXML
    protected Button filterActiveButton;
    @FXML
    protected Button filterInactiveButton;
    @FXML
    protected Button filterBlockedButton;
    @FXML
    protected Button filterPendingButton;

    @FXML
    protected VBox detailPanel;
    @FXML
    protected Label detailInitialsLabel;
    @FXML
    protected Label detailTitleLabel;
    @FXML
    protected Label detailNameLabel;
    @FXML
    protected Label detailEmailLabel;
    @FXML
    protected Label detailStatusLabel;
    @FXML
    protected Label detailRoleLabel;
    @FXML
    protected Label detailPhoneValueLabel;
    @FXML
    protected Label detailCreatedValueLabel;
    @FXML
    protected Label detailCardOneTitleLabel;
    @FXML
    protected Label detailCardOneValueLabel;
    @FXML
    protected Label detailCardTwoTitleLabel;
    @FXML
    protected Label detailCardTwoValueLabel;
    @FXML
    protected Label detailCardThreeTitleLabel;
    @FXML
    protected Label detailCardThreeValueLabel;
    @FXML
    protected Label detailCardFourTitleLabel;
    @FXML
    protected Label detailCardFourValueLabel;
    @FXML
    protected Label detailReferenceTitleLabel;
    @FXML
    protected Label detailReferenceValueLabel;
    @FXML
    protected Label detailExtraOneTitleLabel;
    @FXML
    protected Label detailExtraOneValueLabel;
    @FXML
    protected Label detailExtraTwoTitleLabel;
    @FXML
    protected Label detailExtraTwoValueLabel;
    @FXML
    protected Label detailExtraThreeTitleLabel;
    @FXML
    protected Label detailExtraThreeValueLabel;
    @FXML
    protected Button detailBlockUserButton;

    @FXML
    protected AdminModalIncludeController sharedModalController;

    private final ObservableList<AdminUserDTO> allUsers = FXCollections.observableArrayList();
    private final FilteredList<AdminUserDTO> filteredUsers = new FilteredList<>(allUsers, user -> true);
    private final AdminPdfExportService pdfExportService = new AdminPdfExportService();

    private AdminService adminService;
    private AccountStatus activeStatusFilter;
    private ModalMode modalMode = ModalMode.NONE;
    private AdminUserDTO modalTarget;

    protected abstract UserSectionConfig sectionConfig();

    @Override
    public void setAdminService(AdminService adminService) {
        this.adminService = adminService;
    }

    @Override
    public void onSectionActivated() {
        reloadUsers();
    }

    @FXML
    public void initialize() {
        configureUserModal();
        configureUserTableColumns();
        updateSectionLabels();

        usersTable.setItems(filteredUsers);
        searchField.textProperty().addListener((obs, oldText, newText) -> applyUserFilter());
        usersTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, previous, selected) -> showUserDetails(selected));
        allUsers.addListener((javafx.collections.ListChangeListener<AdminUserDTO>) change -> updateCountLabels());
        filteredUsers.addListener((javafx.collections.ListChangeListener<AdminUserDTO>) change -> updateCountLabels());

        setDetailVisible(false);
        updateFilterChipStyles();
        updateBlockActionButtons(null);
        updateCountLabels();
    }

    private void configureUserModal() {
        sharedModalController.bindActions(this::handleModalCancel, this::handleModalSave,
                this::handleModalConfirmDelete);
        sharedModalController.modalStatusCombo
                .setItems(FXCollections.observableArrayList(AccountStatus.values()));
    }

    @FXML
    public void handleFilterAll() {
        setStatusFilter(null);
    }

    @FXML
    public void handleFilterActive() {
        setStatusFilter(AccountStatus.ACTIVE);
    }

    @FXML
    public void handleFilterInactive() {
        setStatusFilter(AccountStatus.INACTIVE);
    }

    @FXML
    public void handleFilterBlocked() {
        setStatusFilter(AccountStatus.BLOCKED);
    }

    @FXML
    public void handleFilterPending() {
        setStatusFilter(AccountStatus.PENDING);
    }

    @FXML
    public void handleAddUser() {
        modalMode = ModalMode.CREATE;
        modalTarget = null;
        sharedModalController.prepareForForm("Novo " + sectionConfig().badgeLabel().toLowerCase(Locale.ROOT));
        showOnlyUserForm();
        clearUserForm();
    }

    @FXML
    public void handleEditUser() {
        AdminUserDTO selected = getSelectedUser();
        if (selected == null) {
            showFeedback("Selecione um registo primeiro.", true);
            return;
        }
        modalMode = ModalMode.EDIT;
        modalTarget = selected;
        sharedModalController.prepareForForm("Editar " + sectionConfig().badgeLabel().toLowerCase(Locale.ROOT));
        showOnlyUserForm();
        fillUserForm(selected);
    }

    @FXML
    public void handleBlockUser() {
        AdminUserDTO selected = getSelectedUser();
        if (selected == null) {
            showFeedback("Selecione um registo primeiro.", true);
            return;
        }
        boolean isBlocked = selected.status() == AccountStatus.BLOCKED;
        modalMode = isBlocked ? ModalMode.UNBLOCK_CONFIRM : ModalMode.BLOCK_CONFIRM;
        modalTarget = selected;

        sharedModalController.prepareForDeleteConfirm(isBlocked ? "Desbloquear conta" : "Bloquear conta");
        hideAllModalForms();
        sharedModalController.modalDeleteMessageLabel.setText(isBlocked
                ? "Desbloquear " + AdminFormatUtils.fallback(selected.name()) + "? A conta fica ativa novamente."
                : "Bloquear " + AdminFormatUtils.fallback(selected.name()) + "? A conta fica bloqueada.");
    }

    @FXML
    public void handleDeleteUser() {
        AdminUserDTO selected = getSelectedUser();
        if (selected == null) {
            showFeedback("Selecione um registo primeiro.", true);
            return;
        }
        modalMode = ModalMode.DELETE_CONFIRM;
        modalTarget = selected;

        sharedModalController.prepareForDeleteConfirm("Apagar registo");
        hideAllModalForms();
        sharedModalController.modalDeleteMessageLabel.setText(
                "Apagar " + AdminFormatUtils.fallback(selected.name()) + "? Esta acao e permanente.");
    }

    @FXML
    public void handleExportPdf() {
        try {
            Path exportPath = pdfExportService.exportTable(
                    "OBAR - " + sectionConfig().sectionTitle(),
                    "Dados atualmente mostrados na dashboard: " + filteredUsers.size() + " de " + allUsers.size(),
                    "admin_" + sectionConfig().sectionTitle(),
                    userExportColumns(),
                    List.copyOf(filteredUsers));
            showFeedback("PDF exportado: " + exportPath.toAbsolutePath(), false);
        } catch (Exception exception) {
            showFeedback("Falha ao exportar PDF: " + exception.getMessage(), true);
        }
    }

    @FXML
    public void handleCloseDetailPanel() {
        usersTable.getSelectionModel().clearSelection();
        setDetailVisible(false);
    }

    @FXML
    public void handleModalCancel() {
        closeModal();
    }

    @FXML
    public void handleModalSave() {
        if (modalMode == ModalMode.BLOCK_CONFIRM
                || modalMode == ModalMode.UNBLOCK_CONFIRM
                || modalMode == ModalMode.DELETE_CONFIRM) {
            return;
        }

        String password = sharedModalController.modalPasswordField.getText();
        String confirmPassword = sharedModalController.modalConfirmPasswordField.getText();
        if (!AdminFormatUtils.fallback(password).equals(AdminFormatUtils.fallback(confirmPassword))) {
            sharedModalController.showError("Password e confirmacao nao coincidem.");
            return;
        }

        try {
            AdminUserCommand command = new AdminUserCommand(
                    sharedModalController.modalNameField.getText(),
                    sharedModalController.modalEmailField.getText(),
                    sharedModalController.modalPhoneField.getText(),
                    sharedModalController.modalReferenceField.getText(),
                    sharedModalController.modalStatusCombo.getValue(),
                    password,
                    sectionConfig().type());

            if (modalMode == ModalMode.EDIT) {
                if (modalTarget == null || modalTarget.id() == null) {
                    sharedModalController.showError("Registo invalido.");
                    return;
                }
                adminService.updateUser(modalTarget.id(), command);
                finishModalWithSuccess(sectionConfig().badgeLabel() + " atualizado com sucesso.");
                return;
            }

            adminService.createUser(command);
            finishModalWithSuccess(sectionConfig().badgeLabel() + " criado com sucesso.");
        } catch (Exception exception) {
            sharedModalController.showError("Falha ao guardar registo: " + exception.getMessage());
        }
    }

    @FXML
    public void handleModalConfirmDelete() {
        if (modalTarget == null || modalTarget.id() == null) {
            sharedModalController.showError("Registo invalido.");
            return;
        }
        try {
            if (modalMode == ModalMode.DELETE_CONFIRM) {
                adminService.deleteUser(modalTarget.id());
                finishModalWithSuccess("Registo removido com sucesso.");
                return;
            }
            if (modalMode == ModalMode.UNBLOCK_CONFIRM) {
                adminService.unblockUser(modalTarget.id());
                finishModalWithSuccess("Conta desbloqueada com sucesso.");
                return;
            }

            adminService.blockUser(modalTarget.id());
            finishModalWithSuccess("Conta bloqueada com sucesso.");
        } catch (Exception exception) {
            sharedModalController.showError("Falha ao atualizar registo: " + exception.getMessage());
        }
    }

    protected AdminUserDTO getSelectedUser() {
        return usersTable == null ? null : usersTable.getSelectionModel().getSelectedItem();
    }

    protected void reloadUsers() {
        if (adminService == null) {
            return;
        }
        allUsers.setAll(adminService.listUsersByType(sectionConfig().type()));
        applyUserFilter();
        updateCountLabels();
    }

    protected AdminService getAdminService() {
        return adminService;
    }

    protected void showFeedback(String message, boolean isError) {
        if (feedbackLabel == null) {
            return;
        }
        feedbackLabel.setText(message == null ? "" : message);
        feedbackLabel.getStyleClass().removeAll("feedback-success", "feedback-error");
        feedbackLabel.getStyleClass().add(isError ? "feedback-error" : "feedback-success");
    }

    private void setStatusFilter(AccountStatus status) {
        activeStatusFilter = status;
        applyUserFilter();
        updateFilterChipStyles();
        updateCountLabels();
    }

    private void applyUserFilter() {
        String query = AdminFormatUtils.normalize(searchField == null ? "" : searchField.getText());
        filteredUsers.setPredicate(user -> matchesStatus(user) && matchesSearch(user, query));
    }

    private boolean matchesStatus(AdminUserDTO user) {
        if (sectionConfig().type() == UserType.DRIVER
                && activeStatusFilter == AccountStatus.INACTIVE) {
            return !Boolean.TRUE.equals(user.online());
        }
        return activeStatusFilter == null || user.status() == activeStatusFilter;
    }

    private boolean matchesSearch(AdminUserDTO user, String query) {
        if (query.isBlank()) {
            return true;
        }
        return AdminFormatUtils.normalize(user.name()).contains(query)
                || AdminFormatUtils.normalize(user.email()).contains(query)
                || AdminFormatUtils.normalize(user.phone()).contains(query)
                || AdminFormatUtils.normalize(user.licenseNumber()).contains(query)
                || AdminFormatUtils.normalize(user.taxNumber()).contains(query)
                || AdminFormatUtils.normalize(AdminFormatUtils.prettyStatus(user.status())).contains(query);
    }

    private void updateSectionLabels() {
        UserSectionConfig config = sectionConfig();
        addUserButton.setText(config.createLabel());
        searchField.setPromptText(config.searchPrompt());
        metricColumn.setText(config.metricTitle());
        volumeColumn.setText(config.volumeTitle());
    }

    private void updateCountLabels() {
        int total = allUsers.size();
        int shown = filteredUsers.size();
        setButtonText(filterAllButton, "Todos (" + total + ")");
        setButtonText(filterActiveButton, "Ativos (" + count(AccountStatus.ACTIVE) + ")");
        setButtonText(filterInactiveButton, "Offline (" + count(AccountStatus.INACTIVE) + ")");
        setButtonText(filterBlockedButton, "Bloqueados (" + count(AccountStatus.BLOCKED) + ")");
        setButtonText(filterPendingButton, "Pendentes (" + count(AccountStatus.PENDING) + ")");
        setLabelText(listInfoLabel,
                "A mostrar " + shown + " de " + total + " " + sectionConfig().sectionTitle().toLowerCase(Locale.ROOT));
    }

    private long count(AccountStatus status) {
        return allUsers.stream().filter(user -> user.status() == status).count();
    }

    private void updateFilterChipStyles() {
        updateChip(filterAllButton, activeStatusFilter == null);
        updateChip(filterActiveButton, activeStatusFilter == AccountStatus.ACTIVE);
        updateChip(filterInactiveButton, activeStatusFilter == AccountStatus.INACTIVE);
        updateChip(filterBlockedButton, activeStatusFilter == AccountStatus.BLOCKED);
        updateChip(filterPendingButton, activeStatusFilter == AccountStatus.PENDING);
    }

    private void updateChip(Button button, boolean active) {
        if (button == null) {
            return;
        }
        button.getStyleClass().removeAll("filter-chip", "filter-chip-active");
        button.getStyleClass().add(active ? "filter-chip-active" : "filter-chip");
    }

    private void configureUserTableColumns() {
        userIdColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().id() == null ? "-" : "#" + cellData.getValue().id()));
        nameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                AdminFormatUtils.fallback(cellData.getValue().name())));
        emailColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                AdminFormatUtils.fallback(cellData.getValue().email())));
        statusColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                displayedStatus(cellData.getValue())));
        metricColumn.setCellValueFactory(cellData -> new SimpleStringProperty(metricValue(cellData.getValue())));
        volumeColumn.setCellValueFactory(cellData -> new SimpleStringProperty(volumeValue(cellData.getValue())));
        referenceColumn.setCellValueFactory(cellData -> new SimpleStringProperty(referenceValue(cellData.getValue())));
        createdAtColumn.setCellValueFactory(cellData -> {
            LocalDateTime createdAt = cellData.getValue().createdAt();
            return new SimpleStringProperty(createdAt == null ? "-" : createdAt.toString());
        });

        statusColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String statusLabel, boolean empty) {
                super.updateItem(statusLabel, empty);
                getStyleClass().removeAll("status-online", "status-offline", "status-blocked", "status-pending");
                if (empty || statusLabel == null) {
                    setText(null);
                    return;
                }
                setText(statusLabel);
                switch (statusLabel) {
                    case "Online" -> getStyleClass().add("status-online");
                    case "Offline", "Inativo" -> getStyleClass().add("status-offline");
                    case "Bloqueado" -> getStyleClass().add("status-blocked");
                    case "Pendente" -> getStyleClass().add("status-pending");
                    default -> {
                    }
                }
            }
        });
    }

    private void showUserDetails(AdminUserDTO user) {
        if (user == null) {
            updateBlockActionButtons(null);
            setDetailVisible(false);
            return;
        }

        UserSectionConfig config = sectionConfig();
        setLabelText(detailInitialsLabel, AdminFormatUtils.extractInitials(user.name()));
        setLabelText(detailTitleLabel, "Detalhe do " + config.badgeLabel().toLowerCase(Locale.ROOT));
        setLabelText(detailNameLabel, AdminFormatUtils.fallback(user.name()));
        setLabelText(detailEmailLabel, AdminFormatUtils.fallback(user.email()));
        setLabelText(detailStatusLabel, displayedStatus(user));
        setLabelText(detailRoleLabel, config.badgeLabel());
        setLabelText(detailPhoneValueLabel, AdminFormatUtils.fallback(user.phone()));
        setLabelText(detailCreatedValueLabel, user.createdAt() == null ? "-" : user.createdAt().toString());
        setLabelText(detailCardOneTitleLabel, config.detailCardOneTitle());
        setLabelText(detailCardOneValueLabel, cardOneValue(user));
        setLabelText(detailCardTwoTitleLabel, config.detailCardTwoTitle());
        setLabelText(detailCardTwoValueLabel, cardTwoValue(user));
        setLabelText(detailCardThreeTitleLabel, config.detailCardThreeTitle());
        setLabelText(detailCardThreeValueLabel, cardThreeValue(user));
        setLabelText(detailCardFourTitleLabel, config.detailCardFourTitle());
        setLabelText(detailCardFourValueLabel, cardFourValue(user));
        setLabelText(detailReferenceTitleLabel, config.referenceTitle());
        setLabelText(detailReferenceValueLabel, referenceValue(user));
        clearExtraDetailLabels();
        updateBlockActionButtons(user);
        setDetailVisible(true);
    }

    private void updateBlockActionButtons(AdminUserDTO user) {
        boolean isBlocked = user != null && user.status() == AccountStatus.BLOCKED;
        String text = isBlocked ? "Desbloquear" : "Bloquear";
        String detailText = isBlocked ? "Desbloquear conta" : "Bloquear conta";

        configureBlockButton(blockUserButton, isBlocked, (isBlocked ? "\u2713 " : "\u26D4 ") + text);
        configureBlockButton(detailBlockUserButton, isBlocked, (isBlocked ? "\u2713 " : "\u26D4 ") + detailText);
    }

    private void configureBlockButton(Button button, boolean unlockMode, String text) {
        if (button == null) {
            return;
        }
        button.setText(text);
        button.getStyleClass().removeAll("danger", "unlock-button", "detail-action-danger", "detail-action-unlock");
        button.getStyleClass().add(unlockMode
                ? (button == detailBlockUserButton ? "detail-action-unlock" : "unlock-button")
                : (button == detailBlockUserButton ? "detail-action-danger" : "danger"));
    }

    private void showOnlyUserForm() {
        UserSectionConfig config = sectionConfig();
        sharedModalController.modalReferenceLabel.setText(config.referenceTitle().toUpperCase(Locale.ROOT));
        sharedModalController.modalReferenceField.setPromptText(config.referencePrompt());
        AdminModalIncludeController.setVisible(sharedModalController.modalUsersFormSection, true);
        AdminModalIncludeController.setVisible(sharedModalController.modalTripsFormSection, false);
        AdminModalIncludeController.setVisible(sharedModalController.modalTaxRateFormSection, false);
        AdminModalIncludeController.setVisible(sharedModalController.modalDeleteSection, false);
    }

    private void hideAllModalForms() {
        AdminModalIncludeController.setVisible(sharedModalController.modalUsersFormSection, false);
        AdminModalIncludeController.setVisible(sharedModalController.modalTripsFormSection, false);
        AdminModalIncludeController.setVisible(sharedModalController.modalTaxRateFormSection, false);
    }

    private void fillUserForm(AdminUserDTO user) {
        sharedModalController.modalNameField.setText(AdminFormatUtils.fallback(user.name()));
        sharedModalController.modalEmailField.setText(AdminFormatUtils.fallback(user.email()));
        sharedModalController.modalPhoneField.setText(AdminFormatUtils.fallback(user.phone()));
        sharedModalController.modalReferenceField
                .setText(referenceValue(user).equals("-") ? "" : referenceValue(user));
        sharedModalController.modalStatusCombo.setValue(user.status());
        sharedModalController.modalPasswordField.clear();
        sharedModalController.modalConfirmPasswordField.clear();
    }

    private void clearUserForm() {
        sharedModalController.modalNameField.clear();
        sharedModalController.modalEmailField.clear();
        sharedModalController.modalPhoneField.clear();
        sharedModalController.modalReferenceField.clear();
        sharedModalController.modalStatusCombo.setValue(AccountStatus.ACTIVE);
        sharedModalController.modalPasswordField.clear();
        sharedModalController.modalConfirmPasswordField.clear();
    }

    private void finishModalWithSuccess(String message) {
        closeModal();
        reloadUsers();
        showFeedback(message, false);
    }

    private void closeModal() {
        modalMode = ModalMode.NONE;
        modalTarget = null;
        sharedModalController.hide();
    }

    private String metricValue(AdminUserDTO user) {
        return sectionConfig().type() == UserType.DRIVER
                ? AdminFormatUtils.starRating(user.averageRating())
                : AdminFormatUtils.fallback(user.email());
    }

    private List<AdminPdfExportService.PdfColumn<AdminUserDTO>> userExportColumns() {
        if (sectionConfig().type() == UserType.DRIVER) {
            return List.of(
                    column("ID", 0.8f, user -> formatId(user.id())),
                    column("Nome", 2.1f, AdminUserDTO::name),
                    column("Email", 2.4f, AdminUserDTO::email),
                    column("Telefone", 1.3f, AdminUserDTO::phone),
                    column("Estado", 1.2f, this::displayedStatus),
                    column("Licenca", 1.4f, AdminUserDTO::licenseNumber),
                    column("Avaliacao", 1f, user -> AdminFormatUtils.starRating(user.averageRating())),
                    column("Viagens", 0.9f, user -> String.valueOf(AdminFormatUtils.defaultInteger(user.totalTrips()))),
                    column("Disponivel", 1f, user -> Boolean.TRUE.equals(user.available()) ? "Sim" : "Nao"),
                    column("Registado em", 1.6f, user -> formatDate(user.createdAt())),
                    column("Nota", 1.8f, AdminUserDTO::approvalNote));
        }

        return List.of(
                column("ID", 0.8f, user -> formatId(user.id())),
                column("Nome", 2.2f, AdminUserDTO::name),
                column("Email", 2.5f, AdminUserDTO::email),
                column("Telefone", 1.4f, AdminUserDTO::phone),
                column("Estado", 1.2f, user -> AdminFormatUtils.prettyStatus(user.status())),
                column("NIF", 1.4f, AdminUserDTO::taxNumber),
                column("Metodo pagamento", 1.5f, user -> formatId(user.defaultPaymentMethodId())),
                column("Registado em", 1.6f, user -> formatDate(user.createdAt())));
    }

    private String volumeValue(AdminUserDTO user) {
        return sectionConfig().type() == UserType.DRIVER
                ? String.valueOf(AdminFormatUtils.defaultInteger(user.totalTrips()))
                : AdminFormatUtils.fallback(user.phone());
    }

    private String referenceValue(AdminUserDTO user) {
        return sectionConfig().type() == UserType.DRIVER
                ? AdminFormatUtils.fallback(user.licenseNumber())
                : AdminFormatUtils.fallback(user.taxNumber());
    }

    private String cardOneValue(AdminUserDTO user) {
        return sectionConfig().type() == UserType.DRIVER
                ? AdminFormatUtils.starRating(user.averageRating())
                : AdminFormatUtils.prettyStatus(user.status());
    }

    private String cardTwoValue(AdminUserDTO user) {
        return sectionConfig().type() == UserType.DRIVER
                ? String.valueOf(AdminFormatUtils.defaultInteger(user.totalTrips()))
                : AdminFormatUtils.fallback(user.email());
    }

    private String cardThreeValue(AdminUserDTO user) {
        return sectionConfig().type() == UserType.DRIVER
                ? (Boolean.TRUE.equals(user.available()) ? "Sim" : "Nao")
                : (user.defaultPaymentMethodId() == null ? "-" : "#" + user.defaultPaymentMethodId());
    }

    private String cardFourValue(AdminUserDTO user) {
        return sectionConfig().type() == UserType.DRIVER
                ? AdminFormatUtils.prettyStatus(user.status())
                : "Cliente";
    }

    private String displayedStatus(AdminUserDTO user) {
        if (sectionConfig().type() != UserType.DRIVER
                || user.status() == AccountStatus.BLOCKED
                || user.status() == AccountStatus.PENDING) {
            return AdminFormatUtils.prettyStatus(user.status());
        }
        return Boolean.TRUE.equals(user.online()) ? "Online" : "Offline";
    }

    private void clearExtraDetailLabels() {
        setLabelText(detailExtraOneTitleLabel, "");
        setLabelText(detailExtraOneValueLabel, "");
        setLabelText(detailExtraTwoTitleLabel, "");
        setLabelText(detailExtraTwoValueLabel, "");
        setLabelText(detailExtraThreeTitleLabel, "");
        setLabelText(detailExtraThreeValueLabel, "");
    }

    private void setDetailVisible(boolean visible) {
        if (detailPanel != null) {
            detailPanel.setVisible(visible);
            detailPanel.setManaged(visible);
        }
    }

    private void setButtonText(Button button, String text) {
        if (button != null) {
            button.setText(text);
        }
    }

    private void setLabelText(Label label, String text) {
        if (label != null) {
            label.setText(text == null ? "-" : text);
        }
    }

    private String formatId(Integer id) {
        return id == null ? "-" : "#" + id;
    }

    private String formatDate(LocalDateTime value) {
        return value == null ? "-" : EXPORT_DATE_FORMAT.format(value);
    }
}
