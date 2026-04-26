package com.obar.desktop.admin.sections.trips;

import com.obar.bll.admin.AdminTripCommand;
import com.obar.bll.admin.AdminTripDTO;
import com.obar.desktop.admin.sections.trips.TripsViewModel.ModalMode;
import com.obar.desktop.admin.shared.AdminFormatUtils;
import com.obar.desktop.admin.shared.AdminModalController;
import com.obar.desktop.admin.shared.AdminParseUtils;
import com.obar.model.enums.TripStatus;
import com.obar.model.enums.TripType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

/**
 * Presenter for the trip modal dialog.
 */
public final class TripModalPresenter {

    private final TripsViewModel viewModel;
    private final AdminModalController modalController;

    // Form field references - set once by the controller after FXML injection
    private VBox modalTripsFormSection;
    private Label modalDeleteMessageLabel;
    private TextField modalTripClientIdField;
    private TextField modalTripDriverIdField;
    private ComboBox<TripType> modalTripTypeCombo;
    private ComboBox<TripStatus> modalTripStatusCombo;
    private TextField modalTripOriginField;
    private TextField modalTripDestinationField;
    private TextField modalTripEstimatedPriceField;
    private TextField modalTripFinalPriceField;
    private TextField modalTripNotesField;

    public TripModalPresenter(TripsViewModel viewModel, AdminModalController modalController) {
        this.viewModel = viewModel;
        this.modalController = modalController;
    }

    // ---------------------------------------------
    // Field wiring (called once from controller.initialize)
    // ---------------------------------------------

    public void wireFields(
            VBox tripsFormSection,
            Label deleteMessageLabel,
            TextField clientIdField,
            TextField driverIdField,
            ComboBox<TripType> typeCombo,
            ComboBox<TripStatus> statusCombo,
            TextField originField,
            TextField destinationField,
            TextField estimatedPriceField,
            TextField finalPriceField,
            TextField notesField) {
        this.modalTripsFormSection = tripsFormSection;
        this.modalDeleteMessageLabel = deleteMessageLabel;
        this.modalTripClientIdField = clientIdField;
        this.modalTripDriverIdField = driverIdField;
        this.modalTripTypeCombo = typeCombo;
        this.modalTripStatusCombo = statusCombo;
        this.modalTripOriginField = originField;
        this.modalTripDestinationField = destinationField;
        this.modalTripEstimatedPriceField = estimatedPriceField;
        this.modalTripFinalPriceField = finalPriceField;
        this.modalTripNotesField = notesField;
    }

    // ---------------------------------------------
    // Open actions
    // ---------------------------------------------

    public void openCreate() {
        viewModel.openCreateModal();
        modalController.prepareForForm("Nova viagem");
        AdminModalController.toggle(modalTripsFormSection, true);
        clearFields();
    }

    public void openEdit(AdminTripDTO trip) {
        viewModel.openEditModal(trip);
        modalController.prepareForForm("Editar viagem");
        AdminModalController.toggle(modalTripsFormSection, true);
        populateFields(trip);
    }

    public void openDeleteConfirm(AdminTripDTO trip) {
        viewModel.openDeleteModal(trip);
        modalController.prepareForDeleteConfirm("Apagar viagem");
        AdminModalController.toggle(modalTripsFormSection, false);
        modalDeleteMessageLabel.setText(
                "Tem a certeza que deseja apagar a viagem #" + trip.getId()
                        + "?\nRota: " + AdminFormatUtils.fallback(trip.getOriginAddress())
                        + " → " + AdminFormatUtils.fallback(trip.getDestinationAddress()) + ".");
    }

    // ---------------------------------------------
    // Save / delete actions
    // ---------------------------------------------

    /**
     * Persists the form data. Returns a result describing what happened so the
     * controller can show feedback without knowing any persistence details.
     */
    public PersistResult save() {
        try {
            AdminTripCommand command = buildCommand();
            ModalMode mode = viewModel.getModalMode();

            if (mode == ModalMode.EDIT) {
                AdminTripDTO target = viewModel.getModalTarget();
                if (target == null || target.getId() == null) {
                    return PersistResult.error("Viagem invalida.");
                }
                viewModel.getAdminService().updateTrip(target.getId(), command);
                return PersistResult.success("Viagem atualizada com sucesso.");
            }

            viewModel.getAdminService().createTrip(command);
            return PersistResult.success("Viagem criada com sucesso.");

        } catch (Exception exception) {
            return PersistResult.error("Falha ao guardar viagem: " + exception.getMessage());
        }
    }

    /**
     * Deletes the modal target trip.
     */
    public PersistResult confirmDelete() {
        AdminTripDTO target = viewModel.getModalTarget();
        if (target == null || target.getId() == null) {
            return PersistResult.error("Viagem invalida.");
        }
        try {
            viewModel.getAdminService().deleteTrip(target.getId());
            return PersistResult.success("Viagem apagada com sucesso.");
        } catch (Exception exception) {
            return PersistResult.error("Falha ao apagar viagem: " + exception.getMessage());
        }
    }

    public void close() {
        viewModel.closeModal();
        modalController.hide();
    }

    // ---------------------------------------------
    // Private helpers
    // ---------------------------------------------

    private AdminTripCommand buildCommand() {
        return new AdminTripCommand(
                AdminParseUtils.parseRequiredInteger(modalTripClientIdField.getText(), "Cliente ID"),
                AdminParseUtils.parseOptionalInteger(modalTripDriverIdField.getText()),
                modalTripOriginField.getText(),
                modalTripDestinationField.getText(),
                modalTripTypeCombo.getValue(),
                modalTripStatusCombo.getValue(),
                modalTripNotesField.getText(),
                AdminParseUtils.parseOptionalDecimal(modalTripEstimatedPriceField.getText()),
                AdminParseUtils.parseOptionalDecimal(modalTripFinalPriceField.getText()));
    }

    private void populateFields(AdminTripDTO trip) {
        modalTripClientIdField.setText(trip.getClientId() == null ? "" : String.valueOf(trip.getClientId()));
        modalTripDriverIdField.setText(trip.getDriverId() == null ? "" : String.valueOf(trip.getDriverId()));
        modalTripTypeCombo.setValue(trip.getTripType() == null ? TripType.IMMEDIATE : trip.getTripType());
        modalTripStatusCombo.setValue(trip.getStatus() == null ? TripStatus.PENDING : trip.getStatus());
        modalTripOriginField.setText(nullSafe(trip.getOriginAddress()));
        modalTripDestinationField.setText(nullSafe(trip.getDestinationAddress()));
        modalTripEstimatedPriceField
                .setText(trip.getEstimatedPrice() == null ? "" : trip.getEstimatedPrice().toPlainString());
        modalTripFinalPriceField.setText(trip.getFinalPrice() == null ? "" : trip.getFinalPrice().toPlainString());
        modalTripNotesField.setText(nullSafe(trip.getNotes()));
    }

    private void clearFields() {
        modalTripClientIdField.clear();
        modalTripDriverIdField.clear();
        modalTripTypeCombo.setValue(TripType.IMMEDIATE);
        modalTripStatusCombo.setValue(TripStatus.PENDING);
        modalTripOriginField.clear();
        modalTripDestinationField.clear();
        modalTripEstimatedPriceField.clear();
        modalTripFinalPriceField.clear();
        modalTripNotesField.clear();
    }

    private static String nullSafe(String value) {
        return value == null ? "" : value;
    }

    // ---------------------------------------------
    // Result type
    // ---------------------------------------------

    /**
     * Simple result returned by save/delete operations so the controller
     * can react (show feedback, reload) without catching exceptions itself.
     */
    public record PersistResult(boolean success, String message) {
        public static PersistResult success(String message) {
            return new PersistResult(true, message);
        }

        public static PersistResult error(String message) {
            return new PersistResult(false, message);
        }
    }
}
