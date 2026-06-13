package com.obar.desktop.admin.shared;

import com.obar.bll.admin.AdminTaxRateDTO;
import com.obar.model.enums.AccountStatus;
import com.obar.model.enums.TripStatus;
import com.obar.model.enums.TripType;
import java.util.Locale;

import javafx.fxml.FXML;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Controller for the shared admin modal include.
 *
 * <p>
 * The admin screens use the same modal FXML. This controller exposes the modal
 * fields and provides common operations for showing forms, delete
 * confirmations, and validation messages.
 * </p>
 */
public class AdminModalIncludeController {

    @FXML
    private StackPane modalOverlay;
    @FXML
    private Label modalTitleLabel;
    @FXML
    private Label modalSubtitleLabel;
    @FXML
    private Label modalIconLabel;
    @FXML
    private Label modalErrorLabel;

    @FXML
    public VBox modalUsersFormSection;
    @FXML
    public TextField modalNameField;
    @FXML
    public TextField modalEmailField;
    @FXML
    public TextField modalPhoneField;
    @FXML
    public Label modalReferenceLabel;
    @FXML
    public TextField modalReferenceField;
    @FXML
    public ComboBox<AccountStatus> modalStatusCombo;
    @FXML
    public PasswordField modalPasswordField;
    @FXML
    public PasswordField modalConfirmPasswordField;

    @FXML
    public VBox modalTripsFormSection;
    @FXML
    public TextField modalTripClientIdField;
    @FXML
    public TextField modalTripDriverIdField;
    @FXML
    public ComboBox<TripType> modalTripTypeCombo;
    @FXML
    public ComboBox<TripStatus> modalTripStatusCombo;
    @FXML
    public TextField modalTripOriginField;
    @FXML
    public TextField modalTripDestinationField;
    @FXML
    public TextField modalTripEstimatedPriceField;
    @FXML
    public TextField modalTripFinalPriceField;
    @FXML
    public TextField modalTripNotesField;

    @FXML
    public VBox modalTaxRateFormSection;
    @FXML
    public ComboBox<AdminTaxRateDTO> modalTaxRateCombo;
    @FXML
    public TextField modalTaxRateNameField;
    @FXML
    public TextField modalTaxRateValueField;
    @FXML
    public TextField modalTaxRateDescriptionField;
    @FXML
    public CheckBox modalTaxRateActiveCheck;

    @FXML
    public VBox modalDeleteSection;
    @FXML
    public Label modalDeleteMessageLabel;
    @FXML
    private Button modalCloseButton;
    @FXML
    private Button modalCancelButton;
    @FXML
    private Button modalSaveButton;
    @FXML
    private Button modalDeleteConfirmButton;

    public void prepareForForm(String title) {
        configureHeading(title, false);
        setVisible(modalDeleteSection, false);
        setVisible(modalSaveButton, true);
        setVisible(modalDeleteConfirmButton, false);
        setButtonText(modalSaveButton, isCreateTitle(title) ? "Adicionar" : "Guardar");
        clearError();
        show();
    }

    public void prepareForDeleteConfirm(String title) {
        configureHeading(title, true);
        setVisible(modalDeleteSection, true);
        setVisible(modalSaveButton, false);
        setVisible(modalDeleteConfirmButton, true);
        clearError();
        show();
    }

    public void show() {
        setVisible(modalOverlay, true);
    }

    public void hide() {
        setVisible(modalOverlay, false);
        clearError();
    }

    public void setTitle(String title) {
        if (modalTitleLabel != null) {
            modalTitleLabel.setText(title == null ? "" : title);
        }
    }

    public void clearError() {
        if (modalErrorLabel != null) {
            modalErrorLabel.setText("");
            setVisible(modalErrorLabel, false);
        }
    }

    public void showError(String message) {
        if (modalErrorLabel != null) {
            modalErrorLabel.setText(message == null ? "" : message);
            setVisible(modalErrorLabel, true);
        }
    }

    public void bindActions(Runnable onCancel, Runnable onSave, Runnable onDeleteConfirm) {
        bindAction(modalCloseButton, onCancel);
        bindAction(modalCancelButton, onCancel);
        bindAction(modalSaveButton, onSave);
        bindAction(modalDeleteConfirmButton, onDeleteConfirm);
    }

    private void bindAction(Button button, Runnable action) {
        if (button == null || action == null) {
            return;
        }
        button.setOnAction(event -> action.run());
    }

    private void configureHeading(String title, boolean deleteAction) {
        setTitle(title);
        if (modalSubtitleLabel != null) {
            modalSubtitleLabel.setText(resolveSubtitle(title, deleteAction));
        }
        if (modalIconLabel != null) {
            modalIconLabel.setText(resolveIcon(title, deleteAction));
        }
    }

    private String resolveSubtitle(String title, boolean deleteAction) {
        if (deleteAction) {
            return "Confirmar acao no sistema";
        }

        String normalizedTitle = normalize(title);
        boolean createAction = isCreateTitle(title);
        if (normalizedTitle.contains("motorista")) {
            return createAction ? "Novo motorista na equipa" : "Atualizar dados do motorista";
        }
        if (normalizedTitle.contains("cliente")) {
            return createAction ? "Novo cliente na plataforma" : "Atualizar dados do cliente";
        }
        if (normalizedTitle.contains("viagem")) {
            return createAction ? "Nova viagem no sistema" : "Atualizar dados da viagem";
        }
        if (normalizedTitle.contains("taxa")) {
            return "Atualizar dados financeiros";
        }
        return createAction ? "Novo registo no sistema" : "Atualizar dados do registo";
    }

    private String resolveIcon(String title, boolean deleteAction) {
        if (deleteAction) {
            return "X";
        }

        String normalizedTitle = normalize(title);
        if (normalizedTitle.contains("motorista")) {
            return "\uD83D\uDC64";
        }
        if (normalizedTitle.contains("cliente")) {
            return "\uD83D\uDC65";
        }
        if (normalizedTitle.contains("viagem")) {
            return "\uD83D\uDCCD";
        }
        if (normalizedTitle.contains("taxa")) {
            return "\u20AC";
        }
        return "+";
    }

    private boolean isCreateTitle(String title) {
        String normalizedTitle = normalize(title);
        return normalizedTitle.startsWith("novo ") || normalizedTitle.startsWith("nova ");
    }

    private String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT);
    }

    private void setButtonText(Button button, String text) {
        if (button != null) {
            button.setText(text);
        }
    }

    public static void setVisible(Node node, boolean visible) {
        if (node == null) {
            return;
        }
        node.setVisible(visible);
        node.setManaged(visible);
    }
}
