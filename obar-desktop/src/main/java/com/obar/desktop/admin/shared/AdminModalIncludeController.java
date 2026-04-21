package com.obar.desktop.admin.shared;

import javafx.fxml.FXML;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
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
 */
public class AdminModalIncludeController {

    @FXML private StackPane modalOverlay;
    @FXML private Label modalTitleLabel;
    @FXML private Label modalErrorLabel;

    @FXML private VBox modalUsersFormSection;
    @FXML private TextField modalNameField;
    @FXML private TextField modalEmailField;
    @FXML private TextField modalPhoneField;
    @FXML private Label modalReferenceLabel;
    @FXML private TextField modalReferenceField;
    @FXML private ComboBox<?> modalStatusCombo;
    @FXML private PasswordField modalPasswordField;
    @FXML private PasswordField modalConfirmPasswordField;

    @FXML private VBox modalTripsFormSection;
    @FXML private TextField modalTripClientIdField;
    @FXML private TextField modalTripDriverIdField;
    @FXML private ComboBox<?> modalTripTypeCombo;
    @FXML private ComboBox<?> modalTripStatusCombo;
    @FXML private TextField modalTripOriginField;
    @FXML private TextField modalTripDestinationField;
    @FXML private TextField modalTripEstimatedPriceField;
    @FXML private TextField modalTripFinalPriceField;
    @FXML private TextField modalTripNotesField;

    @FXML private VBox modalTaxRateFormSection;
    @FXML private ComboBox<?> modalTaxRateCombo;
    @FXML private TextField modalTaxRateNameField;
    @FXML private TextField modalTaxRateValueField;
    @FXML private TextField modalTaxRateDescriptionField;
    @FXML private CheckBox modalTaxRateActiveCheck;

    @FXML private VBox modalDeleteSection;
    @FXML private Label modalDeleteMessageLabel;
    @FXML private Button modalCancelButton;
    @FXML private Button modalSaveButton;
    @FXML private Button modalDeleteConfirmButton;

    public StackPane getModalOverlay() { return modalOverlay; }
    public Label getModalTitleLabel() { return modalTitleLabel; }
    public Label getModalErrorLabel() { return modalErrorLabel; }

    public VBox getModalUsersFormSection() { return modalUsersFormSection; }
    public TextField getModalNameField() { return modalNameField; }
    public TextField getModalEmailField() { return modalEmailField; }
    public TextField getModalPhoneField() { return modalPhoneField; }
    public Label getModalReferenceLabel() { return modalReferenceLabel; }
    public TextField getModalReferenceField() { return modalReferenceField; }
    @SuppressWarnings("unchecked")
    public <T> ComboBox<T> getModalStatusCombo() { return (ComboBox<T>) modalStatusCombo; }
    public PasswordField getModalPasswordField() { return modalPasswordField; }
    public PasswordField getModalConfirmPasswordField() { return modalConfirmPasswordField; }

    public VBox getModalTripsFormSection() { return modalTripsFormSection; }
    public TextField getModalTripClientIdField() { return modalTripClientIdField; }
    public TextField getModalTripDriverIdField() { return modalTripDriverIdField; }
    @SuppressWarnings("unchecked")
    public <T> ComboBox<T> getModalTripTypeCombo() { return (ComboBox<T>) modalTripTypeCombo; }
    @SuppressWarnings("unchecked")
    public <T> ComboBox<T> getModalTripStatusCombo() { return (ComboBox<T>) modalTripStatusCombo; }
    public TextField getModalTripOriginField() { return modalTripOriginField; }
    public TextField getModalTripDestinationField() { return modalTripDestinationField; }
    public TextField getModalTripEstimatedPriceField() { return modalTripEstimatedPriceField; }
    public TextField getModalTripFinalPriceField() { return modalTripFinalPriceField; }
    public TextField getModalTripNotesField() { return modalTripNotesField; }

    public VBox getModalTaxRateFormSection() { return modalTaxRateFormSection; }
    @SuppressWarnings("unchecked")
    public <T> ComboBox<T> getModalTaxRateCombo() { return (ComboBox<T>) modalTaxRateCombo; }
    public TextField getModalTaxRateNameField() { return modalTaxRateNameField; }
    public TextField getModalTaxRateValueField() { return modalTaxRateValueField; }
    public TextField getModalTaxRateDescriptionField() { return modalTaxRateDescriptionField; }
    public CheckBox getModalTaxRateActiveCheck() { return modalTaxRateActiveCheck; }

    public VBox getModalDeleteSection() { return modalDeleteSection; }
    public Label getModalDeleteMessageLabel() { return modalDeleteMessageLabel; }
    public Button getModalCancelButton() { return modalCancelButton; }
    public Button getModalSaveButton() { return modalSaveButton; }
    public Button getModalDeleteConfirmButton() { return modalDeleteConfirmButton; }

    public AdminModalController createModalController() {
        return new AdminModalController(
                modalOverlay,
                modalTitleLabel,
                modalErrorLabel,
                modalDeleteSection,
                modalSaveButton,
                modalDeleteConfirmButton);
    }

    public void bindActions(Runnable onCancel, Runnable onSave, Runnable onDeleteConfirm) {
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
}