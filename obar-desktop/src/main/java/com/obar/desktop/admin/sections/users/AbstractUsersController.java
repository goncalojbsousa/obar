package com.obar.desktop.admin.sections.users;

import com.obar.bll.admin.AdminService;
import com.obar.bll.admin.AdminUserCommand;
import com.obar.bll.admin.AdminUserDTO;
import com.obar.desktop.admin.sections.AdminSectionController;
import com.obar.desktop.admin.shared.AdminFormatUtils;
import com.obar.desktop.admin.shared.AdminModalController;
import com.obar.desktop.admin.shared.AdminModalIncludeController;
import com.obar.model.enums.AccountStatus;
import com.obar.model.enums.UserType;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;
import java.util.Locale;

public abstract class AbstractUsersController implements AdminSectionController {

    protected final ObservableList<AdminUserDTO> allUsers = FXCollections.observableArrayList();
    protected final FilteredList<AdminUserDTO> filteredUsers = new FilteredList<>(allUsers, user -> true);
    private AccountStatus currentStatusFilter;

    private AdminService adminService;

    @FXML protected VBox root;
    @FXML protected TextField searchField;
    @FXML protected TableView<AdminUserDTO> usersTable;
    @FXML protected TableColumn<AdminUserDTO, String> nameColumn;
    @FXML protected TableColumn<AdminUserDTO, String> userIdColumn;
    @FXML protected TableColumn<AdminUserDTO, String> emailColumn;
    @FXML protected TableColumn<AdminUserDTO, String> statusColumn;
    @FXML protected TableColumn<AdminUserDTO, String> metricColumn;
    @FXML protected TableColumn<AdminUserDTO, String> volumeColumn;
    @FXML protected TableColumn<AdminUserDTO, String> referenceColumn;
    @FXML protected TableColumn<AdminUserDTO, String> createdAtColumn;
    @FXML protected Label listInfoLabel;
    @FXML protected VBox detailPanel;
    @FXML protected Label detailInitialsLabel;
    @FXML protected Label detailTitleLabel;
    @FXML protected Label detailNameLabel;
    @FXML protected Label detailEmailLabel;
    @FXML protected Label detailStatusLabel;
    @FXML protected Label detailRoleLabel;
    @FXML protected Label detailCardOneTitleLabel;
    @FXML protected Label detailCardOneValueLabel;
    @FXML protected Label detailCardTwoTitleLabel;
    @FXML protected Label detailCardTwoValueLabel;
    @FXML protected Label detailCardThreeTitleLabel;
    @FXML protected Label detailCardThreeValueLabel;
    @FXML protected Label detailCardFourTitleLabel;
    @FXML protected Label detailCardFourValueLabel;
    @FXML protected Label detailReferenceTitleLabel;
    @FXML protected Label detailReferenceValueLabel;
    @FXML protected Label detailPhoneValueLabel;
    @FXML protected Label detailCreatedValueLabel;
    @FXML protected Label detailExtraOneTitleLabel;
    @FXML protected Label detailExtraOneValueLabel;
    @FXML protected Label detailExtraTwoTitleLabel;
    @FXML protected Label detailExtraTwoValueLabel;
    @FXML protected Label detailExtraThreeTitleLabel;
    @FXML protected Label detailExtraThreeValueLabel;
    @FXML protected Label feedbackLabel;
    @FXML protected AdminModalIncludeController sharedModalController;
    @FXML protected VBox modalUsersFormSection;
    @FXML protected Label modalDeleteMessageLabel;
    @FXML protected ComboBox<AccountStatus> modalStatusCombo;
    @FXML protected TextField modalNameField;
    @FXML protected TextField modalEmailField;
    @FXML protected TextField modalPhoneField;
    @FXML protected TextField modalReferenceField;
    @FXML protected PasswordField modalPasswordField;
    @FXML protected PasswordField modalConfirmPasswordField;
    @FXML protected Label modalReferenceLabel;
    @FXML protected Button addUserButton;
    @FXML protected Button editUserButton;
    @FXML protected Button deleteUserButton;

    @FXML protected Button filterAllButton;
    @FXML protected Button filterActiveButton;
    @FXML protected Button filterInactiveButton;
    @FXML protected Button filterBlockedButton;
    @FXML protected Button filterPendingButton;

    protected abstract UserType supportedType();
    protected abstract String sectionTitle();
    protected abstract String createLabel();
    protected abstract String badgeLabel();
    protected abstract String searchPrompt();
    protected abstract String referenceTitle();
    protected abstract String referencePrompt();
    protected abstract String metricTitle();
    protected abstract String volumeTitle();
    protected abstract String detailCardOneTitle();
    protected abstract String detailCardTwoTitle();
    protected abstract String detailCardThreeTitle();
    protected abstract String detailCardFourTitle();

    private enum ModalMode {
        NONE,
        CREATE,
        EDIT,
        DELETE_CONFIRM
    }

    private ModalMode modalMode = ModalMode.NONE;
    private AdminUserDTO modalTargetUser;
    private AdminModalController modalController;

    @Override
    public void setAdminService(AdminService adminService) {
        this.adminService = adminService;
    }

    @FXML
    public void initialize() {
        bindModalFields();
        modalController = sharedModalController.createModalController();
        sharedModalController.bindActions(this::handleModalCancel, this::handleModalSave, this::handleModalConfirmDelete);
        usersTable.setItems(filteredUsers);
        modalStatusCombo.setItems(FXCollections.observableArrayList(AccountStatus.values()));
        searchField.textProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        usersTable.getSelectionModel().selectedItemProperty().addListener((obs, previous, current) -> updateDetailsPanel(current));
        setupColumns();
        setDetailsVisible(false);
        refresh();
    }

    private void bindModalFields() {
        if (sharedModalController == null) {
            throw new IllegalStateException("Shared modal controller was not injected.");
        }
        modalUsersFormSection = sharedModalController.getModalUsersFormSection();
        modalDeleteMessageLabel = sharedModalController.getModalDeleteMessageLabel();
        modalStatusCombo = sharedModalController.getModalStatusCombo();
        modalNameField = sharedModalController.getModalNameField();
        modalEmailField = sharedModalController.getModalEmailField();
        modalPhoneField = sharedModalController.getModalPhoneField();
        modalReferenceField = sharedModalController.getModalReferenceField();
        modalPasswordField = sharedModalController.getModalPasswordField();
        modalConfirmPasswordField = sharedModalController.getModalConfirmPasswordField();
        modalReferenceLabel = sharedModalController.getModalReferenceLabel();
    }

    @Override
    public void onSectionActivated() {
        refresh();
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
        openUserFormModal(null);
    }

    @FXML
    public void handleEditUser() {
        AdminUserDTO selectedUser = usersTable.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            showFeedback("Selecione um registo primeiro.", true);
            return;
        }

        openUserFormModal(selectedUser);
    }

    @FXML
    public void handleDeleteUser() {
        AdminUserDTO selectedUser = usersTable.getSelectionModel().getSelectedItem();
        if (selectedUser == null) {
            showFeedback("Selecione um registo primeiro.", true);
            return;
        }

        openDeleteConfirmModal(selectedUser);
    }

    @FXML
    public void handleCloseDetailPanel() {
        usersTable.getSelectionModel().clearSelection();
        setDetailsVisible(false);
    }

    @FXML
    public void handleModalCancel() {
        hideModal();
    }

    @FXML
    public void handleModalSave() {
        if (modalMode == ModalMode.DELETE_CONFIRM) {
            return;
        }

        String password = modalPasswordField.getText();
        String confirmPassword = modalConfirmPasswordField.getText();
        if (!AdminFormatUtils.fallback(password).equals(AdminFormatUtils.fallback(confirmPassword))) {
            showModalError("Password e confirmacao nao coincidem.");
            return;
        }

        if (modalMode == ModalMode.EDIT) {
            persistEditUser();
        } else {
            persistCreateUser();
        }
    }

    @FXML
    public void handleModalConfirmDelete() {
        if (modalMode != ModalMode.DELETE_CONFIRM || modalTargetUser == null) {
            hideModal();
            return;
        }

        try {
            boolean deleted = adminService.blockOrDeleteUser(modalTargetUser.getId());
            showFeedback(deleted ? "Registo removido com sucesso." : "Conta bloqueada com sucesso.", false);
            hideModal();
            refresh();
        } catch (Exception exception) {
            showModalError("Falha ao atualizar registo: " + exception.getMessage());
        }
    }

    protected void setStatusFilter(AccountStatus status) {
        currentStatusFilter = status;
        updateFilterChipState();
        applyFilters();
    }

    protected void refresh() {
        if (adminService == null) {
            return;
        }
        allUsers.setAll(adminService.listUsersByType(supportedType()));
        applyFilters();
        updateFilterLabels();
        updateSectionLabels();
    }

    protected void setupColumns() {
        userIdColumn.setCellValueFactory(cellData -> new SimpleStringProperty(
                cellData.getValue().getId() == null ? "-" : "#" + cellData.getValue().getId()));
        nameColumn.setCellValueFactory(cellData -> new SimpleStringProperty(AdminFormatUtils.fallback(cellData.getValue().getName())));
        emailColumn.setCellValueFactory(cellData -> new SimpleStringProperty(AdminFormatUtils.fallback(cellData.getValue().getEmail())));
        statusColumn.setCellValueFactory(cellData -> new SimpleStringProperty(AdminFormatUtils.prettyStatus(cellData.getValue().getStatus())));
        metricColumn.setCellValueFactory(cellData -> new SimpleStringProperty(metricValue(cellData.getValue())));
        volumeColumn.setCellValueFactory(cellData -> new SimpleStringProperty(volumeValue(cellData.getValue())));
        referenceColumn.setCellValueFactory(cellData -> new SimpleStringProperty(referenceValue(cellData.getValue())));
        createdAtColumn.setCellValueFactory(cellData -> {
            LocalDateTime createdAt = cellData.getValue().getCreatedAt();
            return new SimpleStringProperty(createdAt == null ? "-" : createdAt.toString());
        });
        statusColumn.setCellFactory(column -> new TableCell<>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                getStyleClass().removeAll("status-online", "status-offline", "status-blocked", "status-pending");
                if (empty || item == null) {
                    setText(null);
                    return;
                }
                setText(item);
                if (getIndex() < 0 || getIndex() >= getTableView().getItems().size()) {
                    return;
                }
                AdminUserDTO row = getTableView().getItems().get(getIndex());
                switch (row.getStatus()) {
                    case ACTIVE   -> getStyleClass().add("status-online");
                    case INACTIVE -> getStyleClass().add("status-offline");
                    case BLOCKED  -> getStyleClass().add("status-blocked");
                    case PENDING  -> getStyleClass().add("status-pending");
                }
            }
        });
    }

    protected void applyFilters() {
        String query = AdminFormatUtils.normalize(searchField.getText());
        filteredUsers.setPredicate(user -> matchesStatus(user) && matchesQuery(user, query));
        updateFilterLabels();
    }

    private boolean matchesStatus(AdminUserDTO user) {
        return currentStatusFilter == null || user.getStatus() == currentStatusFilter;
    }

    protected boolean matchesQuery(AdminUserDTO user, String query) {
        if (query.isBlank()) {
            return true;
        }

        return AdminFormatUtils.normalize(user.getName()).contains(query)
                || AdminFormatUtils.normalize(user.getEmail()).contains(query)
                || AdminFormatUtils.normalize(user.getPhone()).contains(query)
                || AdminFormatUtils.normalize(user.getLicenseNumber()).contains(query)
                || AdminFormatUtils.normalize(user.getTaxNumber()).contains(query)
                || AdminFormatUtils.normalize(prettyStatus(user.getStatus())).contains(query);
    }

    protected void updateDetailsPanel(AdminUserDTO user) {
        if (user == null) {
            clearDetails();
            setDetailsVisible(false);
            return;
        }

        setDetailsVisible(true);

        detailInitialsLabel.setText(AdminFormatUtils.extractInitials(user.getName()));
        detailTitleLabel.setText("Detalhe do " + badgeLabel().toLowerCase(Locale.ROOT));
        detailNameLabel.setText(AdminFormatUtils.fallback(user.getName()));
        detailEmailLabel.setText(AdminFormatUtils.fallback(user.getEmail()));
        detailStatusLabel.setText(AdminFormatUtils.prettyStatus(user.getStatus()));
        detailRoleLabel.setText(badgeLabel());
        detailPhoneValueLabel.setText(AdminFormatUtils.fallback(user.getPhone()));
        detailCreatedValueLabel.setText(user.getCreatedAt() == null ? "-" : user.getCreatedAt().toString());

        detailCardOneTitleLabel.setText(detailCardOneTitle());
        detailCardOneValueLabel.setText(cardOneValue(user));
        detailCardTwoTitleLabel.setText(detailCardTwoTitle());
        detailCardTwoValueLabel.setText(cardTwoValue(user));
        detailCardThreeTitleLabel.setText(detailCardThreeTitle());
        detailCardThreeValueLabel.setText(cardThreeValue(user));
        detailCardFourTitleLabel.setText(detailCardFourTitle());
        detailCardFourValueLabel.setText(cardFourValue(user));
        detailReferenceTitleLabel.setText(referenceTitle());
        detailReferenceValueLabel.setText(referenceValue(user));
        setLabelText(detailExtraOneTitleLabel, "");
        setLabelText(detailExtraOneValueLabel, "");
        setLabelText(detailExtraTwoTitleLabel, "");
        setLabelText(detailExtraTwoValueLabel, "");
        setLabelText(detailExtraThreeTitleLabel, "");
        setLabelText(detailExtraThreeValueLabel, "");
    }

    protected void clearDetails() {
        detailInitialsLabel.setText("--");
        detailTitleLabel.setText("Sem selecao");
        detailNameLabel.setText("Sem selecao");
        detailEmailLabel.setText("-");
        detailStatusLabel.setText("-");
        detailRoleLabel.setText(badgeLabel());
        detailPhoneValueLabel.setText("-");
        detailCreatedValueLabel.setText("-");
        detailCardOneTitleLabel.setText(detailCardOneTitle());
        detailCardOneValueLabel.setText("-");
        detailCardTwoTitleLabel.setText(detailCardTwoTitle());
        detailCardTwoValueLabel.setText("-");
        detailCardThreeTitleLabel.setText(detailCardThreeTitle());
        detailCardThreeValueLabel.setText("-");
        detailCardFourTitleLabel.setText(detailCardFourTitle());
        detailCardFourValueLabel.setText("-");
        detailReferenceTitleLabel.setText(referenceTitle());
        detailReferenceValueLabel.setText("-");
        setLabelText(detailExtraOneTitleLabel, "");
        setLabelText(detailExtraOneValueLabel, "-");
        setLabelText(detailExtraTwoTitleLabel, "");
        setLabelText(detailExtraTwoValueLabel, "-");
        setLabelText(detailExtraThreeTitleLabel, "");
        setLabelText(detailExtraThreeValueLabel, "-");
    }

    protected void setDetailsVisible(boolean visible) {
        VBox panel = detailPanel;
        if (panel == null) {
            panel = resolveDetailPanelFromNode(detailTitleLabel);
        }
        if (panel == null) {
            panel = resolveDetailPanelFromNode(detailNameLabel);
        }
        if (panel == null) {
            return;
        }
        detailPanel = panel;
        panel.setVisible(visible);
        panel.setManaged(visible);
    }

    private VBox resolveDetailPanelFromNode(Node node) {
        Node cursor = node;
        while (cursor != null) {
            if (cursor instanceof VBox vbox && vbox.getStyleClass().contains("detail-panel")) {
                return vbox;
            }
            cursor = cursor.getParent();
        }
        return null;
    }

    private void setLabelText(Label label, String value) {
        if (label != null) {
            label.setText(value);
        }
    }

    protected void updateFilterLabels() {
        long activeCount = allUsers.stream().filter(u -> u.getStatus() == AccountStatus.ACTIVE).count();
        long inactiveCount = allUsers.stream().filter(u -> u.getStatus() == AccountStatus.INACTIVE).count();
        long blockedCount = allUsers.stream().filter(u -> u.getStatus() == AccountStatus.BLOCKED).count();
        long pendingCount = allUsers.stream().filter(u -> u.getStatus() == AccountStatus.PENDING).count();

        if (filterAllButton != null)     filterAllButton.setText("Todos (" + allUsers.size() + ")");
        if (filterActiveButton != null)  filterActiveButton.setText("Ativos (" + activeCount + ")");
        if (filterInactiveButton != null) filterInactiveButton.setText("Offline (" + inactiveCount + ")");
        if (filterBlockedButton != null) filterBlockedButton.setText("Bloqueados (" + blockedCount + ")");
        if (filterPendingButton != null) filterPendingButton.setText("Pendentes (" + pendingCount + ")");

        listInfoLabel.setText("A mostrar " + filteredUsers.size() + " de " + allUsers.size() + " " + sectionTitle().toLowerCase(Locale.ROOT));
    }

    protected void updateFilterChipState() {
        setChipState(filterAllButton, currentStatusFilter == null);
        setChipState(filterActiveButton, currentStatusFilter == AccountStatus.ACTIVE);
        setChipState(filterInactiveButton, currentStatusFilter == AccountStatus.INACTIVE);
        setChipState(filterBlockedButton, currentStatusFilter == AccountStatus.BLOCKED);
        setChipState(filterPendingButton, currentStatusFilter == AccountStatus.PENDING);
    }

    private void setChipState(Button button, boolean active) {
        if (button == null) return;
        button.getStyleClass().removeAll("filter-chip", "filter-chip-active");
        button.getStyleClass().add(active ? "filter-chip-active" : "filter-chip");
    }

    protected void updateSectionLabels() {
        addUserButton.setText(createLabel());
        searchField.setPromptText(searchPrompt());
        modalReferenceLabel.setText(referenceTitle());
        modalReferenceField.setPromptText(referencePrompt());
        metricColumn.setText(metricTitle());
        volumeColumn.setText(volumeTitle());
    }

    protected void openUserFormModal(AdminUserDTO editingUser) {
        modalMode = editingUser == null ? ModalMode.CREATE : ModalMode.EDIT;
        modalTargetUser = editingUser;
        modalController.prepareForForm(editingUser == null ? "Novo " + badgeLabel().toLowerCase(Locale.ROOT) : "Editar " + badgeLabel().toLowerCase(Locale.ROOT));
        AdminModalController.toggle(modalUsersFormSection, true);
        modalReferenceLabel.setText(referenceTitle());

        if (editingUser == null) {
            modalNameField.clear();
            modalEmailField.clear();
            modalPhoneField.clear();
            modalReferenceField.clear();
            modalStatusCombo.setValue(AccountStatus.ACTIVE);
        } else {
            modalNameField.setText(AdminFormatUtils.fallback(editingUser.getName()));
            modalEmailField.setText(AdminFormatUtils.fallback(editingUser.getEmail()));
            modalPhoneField.setText(AdminFormatUtils.fallback(editingUser.getPhone()));
            modalReferenceField.setText(supportedType() == UserType.DRIVER ? AdminFormatUtils.fallback(editingUser.getLicenseNumber()) : AdminFormatUtils.fallback(editingUser.getTaxNumber()));
            modalStatusCombo.setValue(editingUser.getStatus());
        }

        modalPasswordField.clear();
        modalConfirmPasswordField.clear();
    }

    protected void openDeleteConfirmModal(AdminUserDTO selectedUser) {
        modalMode = ModalMode.DELETE_CONFIRM;
        modalTargetUser = selectedUser;
        modalController.prepareForDeleteConfirm(selectedUser.getStatus() == AccountStatus.BLOCKED ? "Apagar registo" : "Bloquear conta");
        AdminModalController.toggle(modalUsersFormSection, false);
        modalDeleteMessageLabel.setText(selectedUser.getStatus() == AccountStatus.BLOCKED
                ? "Apagar " + AdminFormatUtils.fallback(selectedUser.getName()) + "? Esta acao e permanente."
                : "Bloquear " + AdminFormatUtils.fallback(selectedUser.getName()) + "? A conta sera marcada como bloqueada.");
    }

    protected void persistCreateUser() {
        try {
            adminService.createUser(new AdminUserCommand(
                    modalNameField.getText(),
                    modalEmailField.getText(),
                    modalPhoneField.getText(),
                    modalReferenceField.getText(),
                    modalStatusCombo.getValue(),
                    modalPasswordField.getText(),
                    supportedType()));
            hideModal();
            refresh();
            showFeedback(badgeLabel() + " criado com sucesso.", false);
        } catch (Exception exception) {
            showModalError("Falha ao criar registo: " + exception.getMessage());
        }
    }

    protected void persistEditUser() {
        try {
            if (modalTargetUser == null || modalTargetUser.getId() == null) {
                showModalError("Registo invalido.");
                return;
            }

            adminService.updateUser(modalTargetUser.getId(), new AdminUserCommand(
                    modalNameField.getText(),
                    modalEmailField.getText(),
                    modalPhoneField.getText(),
                    modalReferenceField.getText(),
                    modalStatusCombo.getValue(),
                    modalPasswordField.getText(),
                    supportedType()));
            hideModal();
            refresh();
            showFeedback(badgeLabel() + " atualizado com sucesso.", false);
        } catch (Exception exception) {
            showModalError("Falha ao atualizar registo: " + exception.getMessage());
        }
    }

    protected void showModal() {
        modalController.show();
    }

    protected void hideModal() {
        modalMode = ModalMode.NONE;
        modalTargetUser = null;
        modalController.hide();
    }

    protected void clearModalError() {
        modalController.clearError();
    }

    protected void showModalError(String message) {
        modalController.showError(message);
    }

    protected void showFeedback(String message, boolean isError) {
        feedbackLabel.setText(message);
        feedbackLabel.getStyleClass().removeAll("feedback-success", "feedback-error");
        feedbackLabel.getStyleClass().add(isError ? "feedback-error" : "feedback-success");
    }

    protected String prettyStatus(AccountStatus status) {
        return AdminFormatUtils.prettyStatus(status);
    }

    protected String metricValue(AdminUserDTO user) {
        return supportedType() == UserType.DRIVER ? AdminFormatUtils.starRating(user.getAverageRating()) : AdminFormatUtils.fallback(user.getEmail());
    }

    protected String volumeValue(AdminUserDTO user) {
        return supportedType() == UserType.DRIVER ? String.valueOf(AdminFormatUtils.defaultInteger(user.getTotalTrips())) : AdminFormatUtils.fallback(user.getPhone());
    }

    protected String referenceValue(AdminUserDTO user) {
        return supportedType() == UserType.DRIVER ? AdminFormatUtils.fallback(user.getLicenseNumber()) : AdminFormatUtils.fallback(user.getTaxNumber());
    }

    protected String cardOneValue(AdminUserDTO user) {
        return supportedType() == UserType.DRIVER ? AdminFormatUtils.starRating(user.getAverageRating()) : AdminFormatUtils.prettyStatus(user.getStatus());
    }

    protected String cardTwoValue(AdminUserDTO user) {
        return supportedType() == UserType.DRIVER ? String.valueOf(AdminFormatUtils.defaultInteger(user.getTotalTrips())) : AdminFormatUtils.fallback(user.getEmail());
    }

    protected String cardThreeValue(AdminUserDTO user) {
        return supportedType() == UserType.DRIVER ? (Boolean.TRUE.equals(user.getAvailable()) ? "Online" : "Offline") : (user.getDefaultPaymentMethodId() == null ? "-" : "#" + user.getDefaultPaymentMethodId());
    }

    protected String cardFourValue(AdminUserDTO user) {
        return supportedType() == UserType.DRIVER ? AdminFormatUtils.prettyStatus(user.getStatus()) : "Cliente";
    }
}
