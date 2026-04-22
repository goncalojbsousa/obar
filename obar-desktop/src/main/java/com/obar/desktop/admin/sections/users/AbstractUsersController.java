package com.obar.desktop.admin.sections.users;

import com.obar.bll.admin.AdminService;
import com.obar.bll.admin.AdminUserCommand;
import com.obar.bll.admin.AdminUserDTO;
import com.obar.desktop.admin.sections.AdminSectionController;
import com.obar.desktop.admin.shared.AdminFormatUtils;
import com.obar.desktop.admin.shared.AdminModalController;
import com.obar.desktop.admin.shared.AdminModalIncludeController;
import com.obar.desktop.admin.shared.DetailPanelBinder;
import com.obar.desktop.admin.shared.DetailPanelBinder.DetailViewModel;
import com.obar.desktop.admin.shared.FilterChipManager;
import com.obar.model.enums.AccountStatus;
import com.obar.model.enums.UserType;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;
import java.util.Locale;

public abstract class AbstractUsersController implements AdminSectionController {

    protected final ObservableList<AdminUserDTO> allUsers = FXCollections.observableArrayList();
    protected final FilteredList<AdminUserDTO> filteredUsers = new FilteredList<>(allUsers, user -> true);

    private AccountStatus currentStatusFilter;
    private AdminService adminService;
    private AdminModalController modalController;
    private DetailPanelBinder detailBinder;
    private FilterChipManager<AccountStatus> statusChips;

    // — modal field references (populated from shared modal in initialize) —
    private ComboBox<AccountStatus> modalStatusCombo;
    private TextField modalNameField;
    private TextField modalEmailField;
    private TextField modalPhoneField;
    private TextField modalReferenceField;
    private Label modalReferenceLabel;
    private PasswordField modalPasswordField;
    private PasswordField modalConfirmPasswordField;
    private VBox modalUsersFormSection;
    private VBox modalDeleteSection;
    private Label modalDeleteMessageLabel;

    // — table & toolbar —
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
    @FXML protected Label feedbackLabel;
    @FXML protected Button addUserButton;
    @FXML protected Button editUserButton;
    @FXML protected Button deleteUserButton;
    @FXML protected Button filterAllButton;
    @FXML protected Button filterActiveButton;
    @FXML protected Button filterInactiveButton;
    @FXML protected Button filterBlockedButton;
    @FXML protected Button filterPendingButton;

    // — detail panel labels (wired into DetailPanelBinder) —
    @FXML protected VBox detailPanel;
    @FXML protected Label detailInitialsLabel;
    @FXML protected Label detailTitleLabel;
    @FXML protected Label detailNameLabel;
    @FXML protected Label detailEmailLabel;
    @FXML protected Label detailStatusLabel;
    @FXML protected Label detailRoleLabel;
    @FXML protected Label detailPhoneValueLabel;
    @FXML protected Label detailCreatedValueLabel;
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
    @FXML protected Label detailExtraOneTitleLabel;
    @FXML protected Label detailExtraOneValueLabel;
    @FXML protected Label detailExtraTwoTitleLabel;
    @FXML protected Label detailExtraTwoValueLabel;
    @FXML protected Label detailExtraThreeTitleLabel;
    @FXML protected Label detailExtraThreeValueLabel;

    // — shared modal include (fx:id="sharedModal" → injects controller as sharedModalController) —
    @FXML protected AdminModalIncludeController sharedModalController;

    // — abstract contract for subclasses —
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

    private enum ModalMode { NONE, CREATE, EDIT, DELETE_CONFIRM }

    private ModalMode modalMode = ModalMode.NONE;
    private AdminUserDTO modalTargetUser;

    @Override
    public void setAdminService(AdminService adminService) {
        this.adminService = adminService;
    }

    @FXML
    public void initialize() {
        bindModalFields();
        modalController = sharedModalController.createModalController();
        sharedModalController.bindActions(this::handleModalCancel, this::handleModalSave, this::handleModalConfirmDelete);
        modalStatusCombo.setItems(FXCollections.observableArrayList(AccountStatus.values()));

        detailBinder = new DetailPanelBinder(
                detailPanel,
                detailInitialsLabel, detailTitleLabel, detailNameLabel, detailEmailLabel,
                detailStatusLabel, detailRoleLabel, detailPhoneValueLabel, detailCreatedValueLabel,
                detailCardOneTitleLabel, detailCardOneValueLabel,
                detailCardTwoTitleLabel, detailCardTwoValueLabel,
                detailCardThreeTitleLabel, detailCardThreeValueLabel,
                detailCardFourTitleLabel, detailCardFourValueLabel,
                detailReferenceTitleLabel, detailReferenceValueLabel,
                detailExtraOneTitleLabel, detailExtraOneValueLabel,
                detailExtraTwoTitleLabel, detailExtraTwoValueLabel,
                detailExtraThreeTitleLabel, detailExtraThreeValueLabel);

        statusChips = new FilterChipManager<AccountStatus>()
                .add(filterAllButton,      null)
                .add(filterActiveButton,   AccountStatus.ACTIVE)
                .add(filterInactiveButton, AccountStatus.INACTIVE)
                .add(filterBlockedButton,  AccountStatus.BLOCKED)
                .add(filterPendingButton,  AccountStatus.PENDING);

        usersTable.setItems(filteredUsers);
        searchField.textProperty().addListener((obs, oldValue, newValue) -> applyFilters());
        usersTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, previous, current) -> onSelectionChanged(current));
        setupColumns();
        detailBinder.setVisible(false);
        refresh();
    }

    private void bindModalFields() {
        if (sharedModalController == null) {
            throw new IllegalStateException("Shared modal controller was not injected.");
        }
        modalUsersFormSection    = sharedModalController.getModalUsersFormSection();
        modalDeleteSection       = sharedModalController.getModalDeleteSection();
        modalDeleteMessageLabel  = sharedModalController.getModalDeleteMessageLabel();
        modalNameField           = sharedModalController.getModalNameField();
        modalEmailField          = sharedModalController.getModalEmailField();
        modalPhoneField          = sharedModalController.getModalPhoneField();
        modalReferenceLabel      = sharedModalController.getModalReferenceLabel();
        modalReferenceField      = sharedModalController.getModalReferenceField();
        modalStatusCombo         = sharedModalController.getModalStatusCombo();
        modalPasswordField       = sharedModalController.getModalPasswordField();
        modalConfirmPasswordField = sharedModalController.getModalConfirmPasswordField();
    }

    @Override
    public void onSectionActivated() {
        refresh();
    }

    // — filter handlers —

    @FXML public void handleFilterAll()      { setStatusFilter(null); }
    @FXML public void handleFilterActive()   { setStatusFilter(AccountStatus.ACTIVE); }
    @FXML public void handleFilterInactive() { setStatusFilter(AccountStatus.INACTIVE); }
    @FXML public void handleFilterBlocked()  { setStatusFilter(AccountStatus.BLOCKED); }
    @FXML public void handleFilterPending()  { setStatusFilter(AccountStatus.PENDING); }

    // — toolbar handlers —

    @FXML public void handleAddUser() { openUserFormModal(null); }

    @FXML
    public void handleEditUser() {
        AdminUserDTO selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showFeedback("Selecione um registo primeiro.", true); return; }
        openUserFormModal(selected);
    }

    @FXML
    public void handleDeleteUser() {
        AdminUserDTO selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) { showFeedback("Selecione um registo primeiro.", true); return; }
        openDeleteConfirmModal(selected);
    }

    @FXML public void handleCloseDetailPanel() {
        usersTable.getSelectionModel().clearSelection();
        detailBinder.clear(emptyState());
    }

    // — modal handlers —

    @FXML public void handleModalCancel() { hideModal(); }

    @FXML
    public void handleModalSave() {
        if (modalMode == ModalMode.DELETE_CONFIRM) { return; }
        String password = modalPasswordField.getText();
        String confirm  = modalConfirmPasswordField.getText();
        if (!AdminFormatUtils.fallback(password).equals(AdminFormatUtils.fallback(confirm))) {
            modalController.showError("Password e confirmacao nao coincidem.");
            return;
        }
        if (modalMode == ModalMode.EDIT) { persistEditUser(); } else { persistCreateUser(); }
    }

    @FXML
    public void handleModalConfirmDelete() {
        if (modalMode != ModalMode.DELETE_CONFIRM || modalTargetUser == null) { hideModal(); return; }
        try {
            boolean deleted = adminService.blockOrDeleteUser(modalTargetUser.getId());
            showFeedback(deleted ? "Registo removido com sucesso." : "Conta bloqueada com sucesso.", false);
            hideModal();
            refresh();
        } catch (Exception exception) {
            modalController.showError("Falha ao atualizar registo: " + exception.getMessage());
        }
    }

    // — private helpers —

    protected void refresh() {
        if (adminService == null) { return; }
        allUsers.setAll(adminService.listUsersByType(supportedType()));
        applyFilters();
        updateFilterLabels();
        updateSectionLabels();
    }

    private void setStatusFilter(AccountStatus status) {
        currentStatusFilter = status;
        statusChips.setActive(status);
        applyFilters();
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
        if (query.isBlank()) { return true; }
        return AdminFormatUtils.normalize(user.getName()).contains(query)
                || AdminFormatUtils.normalize(user.getEmail()).contains(query)
                || AdminFormatUtils.normalize(user.getPhone()).contains(query)
                || AdminFormatUtils.normalize(user.getLicenseNumber()).contains(query)
                || AdminFormatUtils.normalize(user.getTaxNumber()).contains(query)
                || AdminFormatUtils.normalize(AdminFormatUtils.prettyStatus(user.getStatus())).contains(query);
    }

    private void onSelectionChanged(AdminUserDTO user) {
        if (user == null) { detailBinder.clear(emptyState()); return; }
        detailBinder.bind(new DetailViewModel.Builder()
                .initials(AdminFormatUtils.extractInitials(user.getName()))
                .title("Detalhe do " + badgeLabel().toLowerCase(Locale.ROOT))
                .name(AdminFormatUtils.fallback(user.getName()))
                .email(AdminFormatUtils.fallback(user.getEmail()))
                .status(AdminFormatUtils.prettyStatus(user.getStatus()))
                .role(badgeLabel())
                .phone(AdminFormatUtils.fallback(user.getPhone()))
                .created(user.getCreatedAt() == null ? "-" : user.getCreatedAt().toString())
                .card1(detailCardOneTitle(), cardOneValue(user))
                .card2(detailCardTwoTitle(), cardTwoValue(user))
                .card3(detailCardThreeTitle(), cardThreeValue(user))
                .card4(detailCardFourTitle(), cardFourValue(user))
                .ref(referenceTitle(), referenceValue(user))
                .extra1("", "").extra2("", "").extra3("", "")
                .build());
    }

    private DetailViewModel emptyState() {
        return new DetailViewModel.Builder()
                .title(sectionTitle()).role(badgeLabel())
                .card1(detailCardOneTitle(), "-").card2(detailCardTwoTitle(), "-")
                .card3(detailCardThreeTitle(), "-").card4(detailCardFourTitle(), "-")
                .ref(referenceTitle(), "-")
                .build();
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
                if (empty || item == null) { setText(null); return; }
                setText(item);
                if (getIndex() < 0 || getIndex() >= getTableView().getItems().size()) { return; }
                AdminUserDTO row = getTableView().getItems().get(getIndex());
                if (row.getStatus() == null) { return; }
                switch (row.getStatus()) {
                    case ACTIVE   -> getStyleClass().add("status-online");
                    case INACTIVE -> getStyleClass().add("status-offline");
                    case BLOCKED  -> getStyleClass().add("status-blocked");
                    case PENDING  -> getStyleClass().add("status-pending");
                }
            }
        });
    }

    protected void updateFilterLabels() {
        long active   = allUsers.stream().filter(u -> u.getStatus() == AccountStatus.ACTIVE).count();
        long inactive = allUsers.stream().filter(u -> u.getStatus() == AccountStatus.INACTIVE).count();
        long blocked  = allUsers.stream().filter(u -> u.getStatus() == AccountStatus.BLOCKED).count();
        long pending  = allUsers.stream().filter(u -> u.getStatus() == AccountStatus.PENDING).count();

        if (filterAllButton != null)      filterAllButton.setText("Todos (" + allUsers.size() + ")");
        if (filterActiveButton != null)   filterActiveButton.setText("Ativos (" + active + ")");
        if (filterInactiveButton != null) filterInactiveButton.setText("Offline (" + inactive + ")");
        if (filterBlockedButton != null)  filterBlockedButton.setText("Bloqueados (" + blocked + ")");
        if (filterPendingButton != null)  filterPendingButton.setText("Pendentes (" + pending + ")");
        listInfoLabel.setText("A mostrar " + filteredUsers.size() + " de " + allUsers.size()
                + " " + sectionTitle().toLowerCase(Locale.ROOT));
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
        modalController.prepareForForm(editingUser == null
                ? "Novo " + badgeLabel().toLowerCase(Locale.ROOT)
                : "Editar " + badgeLabel().toLowerCase(Locale.ROOT));
        modalReferenceLabel.setText(referenceTitle());
        AdminModalController.toggle(modalUsersFormSection, true);
        AdminModalController.toggle(modalDeleteSection, false);

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
            modalReferenceField.setText(supportedType() == UserType.DRIVER
                    ? AdminFormatUtils.fallback(editingUser.getLicenseNumber())
                    : AdminFormatUtils.fallback(editingUser.getTaxNumber()));
            modalStatusCombo.setValue(editingUser.getStatus());
        }
        modalPasswordField.clear();
        modalConfirmPasswordField.clear();
        modalController.clearError();
    }

    protected void openDeleteConfirmModal(AdminUserDTO selectedUser) {
        modalMode = ModalMode.DELETE_CONFIRM;
        modalTargetUser = selectedUser;
        modalController.prepareForDeleteConfirm(
                selectedUser.getStatus() == AccountStatus.BLOCKED ? "Apagar registo" : "Bloquear conta");
        AdminModalController.toggle(modalUsersFormSection, false);
        AdminModalController.toggle(modalDeleteSection, true);
        modalDeleteMessageLabel.setText(selectedUser.getStatus() == AccountStatus.BLOCKED
                ? "Apagar " + AdminFormatUtils.fallback(selectedUser.getName()) + "? Esta acao e permanente."
                : "Bloquear " + AdminFormatUtils.fallback(selectedUser.getName()) + "? A conta sera marcada como bloqueada.");
        modalController.clearError();
    }

    protected void persistCreateUser() {
        try {
            adminService.createUser(new AdminUserCommand(
                    modalNameField.getText(), modalEmailField.getText(),
                    modalPhoneField.getText(), modalReferenceField.getText(),
                    modalStatusCombo.getValue(), modalPasswordField.getText(), supportedType()));
            hideModal();
            refresh();
            showFeedback(badgeLabel() + " criado com sucesso.", false);
        } catch (Exception exception) {
            modalController.showError("Falha ao criar registo: " + exception.getMessage());
        }
    }

    protected void persistEditUser() {
        try {
            if (modalTargetUser == null || modalTargetUser.getId() == null) {
                modalController.showError("Registo invalido.");
                return;
            }
            adminService.updateUser(modalTargetUser.getId(), new AdminUserCommand(
                    modalNameField.getText(), modalEmailField.getText(),
                    modalPhoneField.getText(), modalReferenceField.getText(),
                    modalStatusCombo.getValue(), modalPasswordField.getText(), supportedType()));
            hideModal();
            refresh();
            showFeedback(badgeLabel() + " atualizado com sucesso.", false);
        } catch (Exception exception) {
            modalController.showError("Falha ao atualizar registo: " + exception.getMessage());
        }
    }

    protected void hideModal() {
        modalMode = ModalMode.NONE;
        modalTargetUser = null;
        modalController.hide();
    }

    protected void showFeedback(String message, boolean isError) {
        feedbackLabel.setText(message);
        feedbackLabel.getStyleClass().removeAll("feedback-success", "feedback-error");
        feedbackLabel.getStyleClass().add(isError ? "feedback-error" : "feedback-success");
    }

    // — column value helpers —

    protected String metricValue(AdminUserDTO user) {
        return supportedType() == UserType.DRIVER
                ? AdminFormatUtils.starRating(user.getAverageRating())
                : AdminFormatUtils.fallback(user.getEmail());
    }

    protected String volumeValue(AdminUserDTO user) {
        return supportedType() == UserType.DRIVER
                ? String.valueOf(AdminFormatUtils.defaultInteger(user.getTotalTrips()))
                : AdminFormatUtils.fallback(user.getPhone());
    }

    protected String referenceValue(AdminUserDTO user) {
        return supportedType() == UserType.DRIVER
                ? AdminFormatUtils.fallback(user.getLicenseNumber())
                : AdminFormatUtils.fallback(user.getTaxNumber());
    }

    protected String cardOneValue(AdminUserDTO user) {
        return supportedType() == UserType.DRIVER
                ? AdminFormatUtils.starRating(user.getAverageRating())
                : AdminFormatUtils.prettyStatus(user.getStatus());
    }

    protected String cardTwoValue(AdminUserDTO user) {
        return supportedType() == UserType.DRIVER
                ? String.valueOf(AdminFormatUtils.defaultInteger(user.getTotalTrips()))
                : AdminFormatUtils.fallback(user.getEmail());
    }

    protected String cardThreeValue(AdminUserDTO user) {
        return supportedType() == UserType.DRIVER
                ? (Boolean.TRUE.equals(user.getAvailable()) ? "Online" : "Offline")
                : (user.getDefaultPaymentMethodId() == null ? "-" : "#" + user.getDefaultPaymentMethodId());
    }

    protected String cardFourValue(AdminUserDTO user) {
        return supportedType() == UserType.DRIVER
                ? AdminFormatUtils.prettyStatus(user.getStatus())
                : "Cliente";
    }
}
