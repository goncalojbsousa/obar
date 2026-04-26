package com.obar.desktop.admin.sections.users;

import com.obar.bll.admin.AdminService;
import com.obar.bll.admin.AdminUserDTO;
import com.obar.desktop.admin.sections.AdminSectionController;
import com.obar.desktop.admin.shared.AdminFormatUtils;
import com.obar.desktop.admin.shared.AdminModalController;
import com.obar.desktop.admin.shared.AdminModalIncludeController;
import com.obar.desktop.admin.shared.DetailPanelBinder;
import com.obar.desktop.admin.shared.FilterChipManager;
import com.obar.desktop.admin.sections.users.UserModalPresenter.PersistResult;
import com.obar.model.enums.AccountStatus;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.time.LocalDateTime;
import java.util.Locale;

/**
 * Abstract controller for user admin sections (Drivers, Clients).
 *
 * <p>
 * Responsibilities (only these):
 * <ul>
 * <li>Declare {@code @FXML} fields and wire them on {@code initialize()}.
 * <li>Dispatch user actions to {@link UsersViewModel} or
 * {@link UserModalPresenter}.
 * <li>React to ViewModel state changes to update labels and chips.
 * </ul>
 *
 * <p>
 * Subclasses provide the concrete ViewModel via {@link #createViewModel()},
 * which replaces the previous abstract label/type methods on this class.
 */
public abstract class AbstractUsersController implements AdminSectionController {

    // - table & toolbar -
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
    protected Button editUserButton;
    @FXML
    protected Button deleteUserButton;
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

    // - detail panel -
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

    // - shared modal include -
    @FXML
    protected AdminModalIncludeController sharedModalController;

    // - collaborators -
    private UsersViewModel viewModel;
    private UserModalPresenter modalPresenter;
    private DetailPanelBinder detailBinder;
    private FilterChipManager<AccountStatus> statusChips;

    // ---------------------------------------------
    // Subclass contract: provide the concrete ViewModel
    // ---------------------------------------------

    /**
     * Returns the concrete ViewModel for this section.
     * Called once during {@code initialize()} - subclasses just
     * {@code return new DriversViewModel()}.
     */
    protected abstract UsersViewModel createViewModel();

    // ---------------------------------------------
    // AdminSectionController contract
    // ---------------------------------------------

    @Override
    public void setAdminService(AdminService adminService) {
        if (viewModel != null) {
            viewModel.init(adminService);
        }
    }

    @Override
    public void onSectionActivated() {
        if (viewModel != null) {
            viewModel.reload();
            updateSectionLabels();
            updateCountLabels();
        }
    }

    // ---------------------------------------------
    // Initialization
    // ---------------------------------------------

    @FXML
    public void initialize() {
        viewModel = createViewModel();

        AdminModalController modalController = sharedModalController.createModalController();
        sharedModalController.bindActions(this::handleModalCancel, this::handleModalSave,
                this::handleModalConfirmDelete);

        modalPresenter = new UserModalPresenter(viewModel, modalController);
        wireModalPresenterFields();

        sharedModalController.<AccountStatus>getModalStatusCombo()
                .setItems(FXCollections.observableArrayList(AccountStatus.values()));

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
                .add(filterAllButton, null)
                .add(filterActiveButton, AccountStatus.ACTIVE)
                .add(filterInactiveButton, AccountStatus.INACTIVE)
                .add(filterBlockedButton, AccountStatus.BLOCKED)
                .add(filterPendingButton, AccountStatus.PENDING);

        usersTable.setItems(viewModel.getFilteredUsers());
        searchField.textProperty().bindBidirectional(viewModel.searchQueryProperty());

        usersTable.getSelectionModel().selectedItemProperty()
                .addListener((obs, previous, current) -> onSelectionChanged(current));

        viewModel.activeStatusFilterProperty()
                .addListener((obs, previous, current) -> {
                    statusChips.setActive(current);
                    updateCountLabels();
                });

        viewModel.getAllUsers()
                .addListener((javafx.collections.ListChangeListener<AdminUserDTO>) change -> updateCountLabels());

        setupColumns();
        updateSectionLabels();
        detailBinder.setVisible(false);
    }

    private void wireModalPresenterFields() {
        modalPresenter.wireFields(
                sharedModalController.getModalUsersFormSection(),
                sharedModalController.getModalDeleteSection(),
                sharedModalController.getModalDeleteMessageLabel(),
                sharedModalController.getModalReferenceLabel(),
                sharedModalController.getModalNameField(),
                sharedModalController.getModalEmailField(),
                sharedModalController.getModalPhoneField(),
                sharedModalController.getModalReferenceField(),
                sharedModalController.getModalStatusCombo(),
                sharedModalController.getModalPasswordField(),
                sharedModalController.getModalConfirmPasswordField());
    }

    // ---------------------------------------------
    // Filter handlers
    // ---------------------------------------------

    @FXML
    public void handleFilterAll() {
        viewModel.setStatusFilter(null);
    }

    @FXML
    public void handleFilterActive() {
        viewModel.setStatusFilter(AccountStatus.ACTIVE);
    }

    @FXML
    public void handleFilterInactive() {
        viewModel.setStatusFilter(AccountStatus.INACTIVE);
    }

    @FXML
    public void handleFilterBlocked() {
        viewModel.setStatusFilter(AccountStatus.BLOCKED);
    }

    @FXML
    public void handleFilterPending() {
        viewModel.setStatusFilter(AccountStatus.PENDING);
    }

    // ---------------------------------------------
    // Toolbar handlers
    // ---------------------------------------------

    @FXML
    public void handleAddUser() {
        modalPresenter.openCreate();
    }

    @FXML
    public void handleEditUser() {
        AdminUserDTO selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showFeedback("Selecione um registo primeiro.", true);
            return;
        }
        modalPresenter.openEdit(selected);
    }

    @FXML
    public void handleDeleteUser() {
        AdminUserDTO selected = usersTable.getSelectionModel().getSelectedItem();
        if (selected == null) {
            showFeedback("Selecione um registo primeiro.", true);
            return;
        }
        modalPresenter.openDeleteConfirm(selected);
    }

    @FXML
    public void handleCloseDetailPanel() {
        usersTable.getSelectionModel().clearSelection();
        detailBinder.clear(UserDetailMapper.empty(viewModel));
    }

    // ---------------------------------------------
    // Modal handlers
    // ---------------------------------------------

    @FXML
    public void handleModalCancel() {
        modalPresenter.close();
    }

    @FXML
    public void handleModalSave() {
        if (viewModel.getModalMode() == UsersViewModel.ModalMode.DELETE_CONFIRM) {
            return;
        }
        PersistResult result = modalPresenter.save();
        if (result.success()) {
            modalPresenter.close();
            viewModel.reload();
            updateCountLabels();
            showFeedback(result.message(), false);
        } else if (!result.silent()) {
            showFeedback(result.message(), true);
        }
    }

    @FXML
    public void handleModalConfirmDelete() {
        PersistResult result = modalPresenter.confirmDelete();
        if (result.success()) {
            modalPresenter.close();
            viewModel.reload();
            updateCountLabels();
            showFeedback(result.message(), false);
        } else if (!result.silent()) {
            showFeedback(result.message(), true);
        }
    }

    // ---------------------------------------------
    // Private helpers
    // ---------------------------------------------

    private void onSelectionChanged(AdminUserDTO user) {
        if (user == null) {
            detailBinder.clear(UserDetailMapper.empty(viewModel));
            return;
        }
        detailBinder.bind(UserDetailMapper.from(user, viewModel));
    }

    private void updateCountLabels() {
        long active = count(AccountStatus.ACTIVE);
        long inactive = count(AccountStatus.INACTIVE);
        long blocked = count(AccountStatus.BLOCKED);
        long pending = count(AccountStatus.PENDING);
        int total = viewModel.getAllUsers().size();
        int shown = viewModel.getFilteredUsers().size();

        if (filterAllButton != null)
            filterAllButton.setText("Todos (" + total + ")");
        if (filterActiveButton != null)
            filterActiveButton.setText("Ativos (" + active + ")");
        if (filterInactiveButton != null)
            filterInactiveButton.setText("Offline (" + inactive + ")");
        if (filterBlockedButton != null)
            filterBlockedButton.setText("Bloqueados (" + blocked + ")");
        if (filterPendingButton != null)
            filterPendingButton.setText("Pendentes (" + pending + ")");
        if (listInfoLabel != null)
            listInfoLabel.setText(
                    "A mostrar " + shown + " de " + total + " " + viewModel.sectionTitle().toLowerCase(Locale.ROOT));
    }

    private long count(AccountStatus status) {
        return viewModel.getAllUsers().stream()
                .filter(u -> u.getStatus() == status)
                .count();
    }

    private void updateSectionLabels() {
        if (addUserButton != null)
            addUserButton.setText(viewModel.createLabel());
        if (searchField != null)
            searchField.setPromptText(viewModel.searchPrompt());
        if (metricColumn != null)
            metricColumn.setText(viewModel.metricTitle());
        if (volumeColumn != null)
            volumeColumn.setText(viewModel.volumeTitle());
    }

    private void setupColumns() {
        userIdColumn.setCellValueFactory(cd -> new SimpleStringProperty(
                cd.getValue().getId() == null ? "-" : "#" + cd.getValue().getId()));
        nameColumn.setCellValueFactory(cd -> new SimpleStringProperty(
                AdminFormatUtils.fallback(cd.getValue().getName())));
        emailColumn.setCellValueFactory(cd -> new SimpleStringProperty(
                AdminFormatUtils.fallback(cd.getValue().getEmail())));
        statusColumn.setCellValueFactory(cd -> new SimpleStringProperty(
                AdminFormatUtils.prettyStatus(cd.getValue().getStatus())));
        metricColumn.setCellValueFactory(cd -> new SimpleStringProperty(
                viewModel.metricValue(cd.getValue())));
        volumeColumn.setCellValueFactory(cd -> new SimpleStringProperty(
                viewModel.volumeValue(cd.getValue())));
        referenceColumn.setCellValueFactory(cd -> new SimpleStringProperty(
                viewModel.referenceValue(cd.getValue())));
        createdAtColumn.setCellValueFactory(cd -> {
            LocalDateTime t = cd.getValue().getCreatedAt();
            return new SimpleStringProperty(t == null ? "-" : t.toString());
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
                if (row.getStatus() == null) {
                    return;
                }
                switch (row.getStatus()) {
                    case ACTIVE -> getStyleClass().add("status-online");
                    case INACTIVE -> getStyleClass().add("status-offline");
                    case BLOCKED -> getStyleClass().add("status-blocked");
                    case PENDING -> getStyleClass().add("status-pending");
                }
            }
        });
    }

    private void showFeedback(String message, boolean isError) {
        if (feedbackLabel == null) {
            return;
        }
        feedbackLabel.setText(message);
        feedbackLabel.getStyleClass().removeAll("feedback-success", "feedback-error");
        feedbackLabel.getStyleClass().add(isError ? "feedback-error" : "feedback-success");
    }
}
