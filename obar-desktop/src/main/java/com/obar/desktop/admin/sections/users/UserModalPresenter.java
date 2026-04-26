package com.obar.desktop.admin.sections.users;

import com.obar.bll.admin.AdminUserCommand;
import com.obar.bll.admin.AdminUserDTO;
import com.obar.desktop.admin.sections.users.UsersViewModel.ModalMode;
import com.obar.desktop.admin.shared.AdminFormatUtils;
import com.obar.desktop.admin.shared.AdminModalController;
import com.obar.model.enums.AccountStatus;
import com.obar.model.enums.UserType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/**
 * Presenter for the user modal dialog (create, edit, block/delete).
 *
 * <p>
 * Manages the form field lifecycle: populating on open, reading on save,
 * and delegating persistence to the service via the ViewModel.
 * The controller wires FXML nodes into this presenter once on initialize.
 */
public final class UserModalPresenter {

    private final UsersViewModel viewModel;
    private final AdminModalController modalController;

    // Form field references - set once by the controller after FXML injection
    private VBox modalUsersFormSection;
    private VBox modalDeleteSection;
    private Label modalDeleteMessageLabel;
    private Label modalReferenceLabel;
    private TextField modalNameField;
    private TextField modalEmailField;
    private TextField modalPhoneField;
    private TextField modalReferenceField;
    private ComboBox<AccountStatus> modalStatusCombo;
    private PasswordField modalPasswordField;
    private PasswordField modalConfirmPasswordField;

    public UserModalPresenter(UsersViewModel viewModel, AdminModalController modalController) {
        this.viewModel = viewModel;
        this.modalController = modalController;
    }

    // ---------------------------------------------
    // Field wiring (called once from controller.initialize)
    // ---------------------------------------------

    public void wireFields(
            VBox usersFormSection,
            VBox deleteSection,
            Label deleteMessageLabel,
            Label referenceLabel,
            TextField nameField,
            TextField emailField,
            TextField phoneField,
            TextField referenceField,
            ComboBox<AccountStatus> statusCombo,
            PasswordField passwordField,
            PasswordField confirmPasswordField) {
        this.modalUsersFormSection = usersFormSection;
        this.modalDeleteSection = deleteSection;
        this.modalDeleteMessageLabel = deleteMessageLabel;
        this.modalReferenceLabel = referenceLabel;
        this.modalNameField = nameField;
        this.modalEmailField = emailField;
        this.modalPhoneField = phoneField;
        this.modalReferenceField = referenceField;
        this.modalStatusCombo = statusCombo;
        this.modalPasswordField = passwordField;
        this.modalConfirmPasswordField = confirmPasswordField;
    }

    // ---------------------------------------------
    // Open actions
    // ---------------------------------------------

    public void openCreate() {
        viewModel.openCreateModal();
        modalReferenceLabel.setText(viewModel.referenceTitle());
        modalController.prepareForForm("Novo " + viewModel.badgeLabel().toLowerCase());
        AdminModalController.toggle(modalUsersFormSection, true);
        AdminModalController.toggle(modalDeleteSection, false);
        clearFields();
        modalController.clearError();
    }

    public void openEdit(AdminUserDTO user) {
        viewModel.openEditModal(user);
        modalReferenceLabel.setText(viewModel.referenceTitle());
        modalController.prepareForForm("Editar " + viewModel.badgeLabel().toLowerCase());
        AdminModalController.toggle(modalUsersFormSection, true);
        AdminModalController.toggle(modalDeleteSection, false);
        populateFields(user);
        modalController.clearError();
    }

    public void openDeleteConfirm(AdminUserDTO user) {
        viewModel.openDeleteModal(user);
        boolean alreadyBlocked = user.getStatus() == AccountStatus.BLOCKED;
        modalController.prepareForDeleteConfirm(alreadyBlocked ? "Apagar registo" : "Bloquear conta");
        AdminModalController.toggle(modalUsersFormSection, false);
        AdminModalController.toggle(modalDeleteSection, true);
        modalDeleteMessageLabel.setText(alreadyBlocked
                ? "Apagar " + AdminFormatUtils.fallback(user.getName()) + "? Esta acao e permanente."
                : "Bloquear " + AdminFormatUtils.fallback(user.getName()) + "? A conta sera marcada como bloqueada.");
        modalController.clearError();
    }

    // ---------------------------------------------
    // Save / delete actions
    // ---------------------------------------------

    /**
     * Validates passwords and persists the form data.
     * Returns a result so the controller can show feedback without catching
     * exceptions.
     */
    public PersistResult save() {
        String password = modalPasswordField.getText();
        String confirm = modalConfirmPasswordField.getText();

        if (!AdminFormatUtils.fallback(password).equals(AdminFormatUtils.fallback(confirm))) {
            modalController.showError("Password e confirmacao nao coincidem.");
            return PersistResult.handledError(); // error already shown in modal
        }

        ModalMode mode = viewModel.getModalMode();
        try {
            AdminUserCommand command = buildCommand();
            if (mode == ModalMode.EDIT) {
                AdminUserDTO target = viewModel.getModalTarget();
                if (target == null || target.getId() == null) {
                    modalController.showError("Registo invalido.");
                    return PersistResult.handledError();
                }
                viewModel.getAdminService().updateUser(target.getId(), command);
                return PersistResult.ok(viewModel.badgeLabel() + " atualizado com sucesso.");
            }
            viewModel.getAdminService().createUser(command);
            return PersistResult.ok(viewModel.badgeLabel() + " criado com sucesso.");
        } catch (Exception exception) {
            modalController.showError("Falha ao guardar registo: " + exception.getMessage());
            return PersistResult.handledError();
        }
    }

    /**
     * Blocks or deletes the modal target user depending on current status.
     */
    public PersistResult confirmDelete() {
        AdminUserDTO target = viewModel.getModalTarget();
        if (target == null || target.getId() == null) {
            modalController.showError("Registo invalido.");
            return PersistResult.handledError();
        }
        try {
            boolean deleted = viewModel.getAdminService().blockOrDeleteUser(target.getId());
            return PersistResult.ok(deleted
                    ? "Registo removido com sucesso."
                    : "Conta bloqueada com sucesso.");
        } catch (Exception exception) {
            modalController.showError("Falha ao atualizar registo: " + exception.getMessage());
            return PersistResult.handledError();
        }
    }

    public void close() {
        viewModel.closeModal();
        modalController.hide();
    }

    // ---------------------------------------------
    // Private helpers
    // ---------------------------------------------

    private AdminUserCommand buildCommand() {
        return new AdminUserCommand(
                modalNameField.getText(),
                modalEmailField.getText(),
                modalPhoneField.getText(),
                modalReferenceField.getText(),
                modalStatusCombo.getValue(),
                modalPasswordField.getText(),
                viewModel.supportedType());
    }

    private void populateFields(AdminUserDTO user) {
        modalNameField.setText(AdminFormatUtils.fallback(user.getName()));
        modalEmailField.setText(AdminFormatUtils.fallback(user.getEmail()));
        modalPhoneField.setText(AdminFormatUtils.fallback(user.getPhone()));
        modalReferenceField.setText(viewModel.supportedType() == UserType.DRIVER
                ? AdminFormatUtils.fallback(user.getLicenseNumber())
                : AdminFormatUtils.fallback(user.getTaxNumber()));
        modalStatusCombo.setValue(user.getStatus());
        modalPasswordField.clear();
        modalConfirmPasswordField.clear();
    }

    private void clearFields() {
        modalNameField.clear();
        modalEmailField.clear();
        modalPhoneField.clear();
        modalReferenceField.clear();
        modalStatusCombo.setValue(AccountStatus.ACTIVE);
        modalPasswordField.clear();
        modalConfirmPasswordField.clear();
    }

    // ---------------------------------------------
    // Result type
    // ---------------------------------------------

    /**
     * Result returned by save/delete so the controller can react without
     * catching exceptions or knowing persistence details.
     *
     * <p>
     * {@code silent} means the error was already shown inside the modal
     * and the controller should not overwrite it with a feedback bar message.
     */
    public record PersistResult(boolean success, String message, boolean silent) {
        public static PersistResult ok(String message) {
            return new PersistResult(true, message, false);
        }

        public static PersistResult handledError() {
            return new PersistResult(false, null, true);
        }
    }
}
