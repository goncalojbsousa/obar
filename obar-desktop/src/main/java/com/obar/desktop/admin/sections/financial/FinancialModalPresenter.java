package com.obar.desktop.admin.sections.financial;

import com.obar.bll.admin.AdminTaxRateCommand;
import com.obar.bll.admin.AdminTaxRateDTO;
import com.obar.desktop.admin.shared.AdminFormatUtils;
import com.obar.desktop.admin.shared.AdminModalController;
import com.obar.desktop.admin.shared.AdminParseUtils;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.TextField;
import javafx.scene.layout.VBox;

import java.math.BigDecimal;

/**
 * Presenter for financial tax-rate modal operations.
 */
public final class FinancialModalPresenter {

    private final FinancialViewModel viewModel;
    private final AdminModalController modalController;

    private VBox modalTaxRateFormSection;
    private Label modalDeleteMessageLabel;
    private ComboBox<AdminTaxRateDTO> modalTaxRateCombo;
    private TextField modalTaxRateNameField;
    private TextField modalTaxRateValueField;
    private TextField modalTaxRateDescriptionField;
    private CheckBox modalTaxRateActiveCheck;

    public FinancialModalPresenter(FinancialViewModel viewModel, AdminModalController modalController) {
        this.viewModel = viewModel;
        this.modalController = modalController;
    }

    public void wireFields(
            VBox taxRateFormSection,
            Label deleteMessageLabel,
            ComboBox<AdminTaxRateDTO> taxRateCombo,
            TextField taxRateNameField,
            TextField taxRateValueField,
            TextField taxRateDescriptionField,
            CheckBox taxRateActiveCheck) {
        this.modalTaxRateFormSection = taxRateFormSection;
        this.modalDeleteMessageLabel = deleteMessageLabel;
        this.modalTaxRateCombo = taxRateCombo;
        this.modalTaxRateNameField = taxRateNameField;
        this.modalTaxRateValueField = taxRateValueField;
        this.modalTaxRateDescriptionField = taxRateDescriptionField;
        this.modalTaxRateActiveCheck = taxRateActiveCheck;
    }

    public void initialize() {
        modalTaxRateCombo.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(AdminTaxRateDTO item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : AdminFormatUtils.formatTaxRateDisplay(item));
            }
        });
        modalTaxRateCombo.setButtonCell(new ListCell<>() {
            @Override
            protected void updateItem(AdminTaxRateDTO item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : AdminFormatUtils.formatTaxRateDisplay(item));
            }
        });
        modalTaxRateCombo.valueProperty().addListener((obs, oldValue, newValue) -> populateFields(newValue));
    }

    public PersistResult openEditTaxRate() {
        if (viewModel.getAllTaxRates().isEmpty()) {
            return PersistResult.fail("Nao existem taxas de IVA para editar.");
        }

        modalController.prepareForForm("Editar taxa de IVA");
        AdminModalController.toggle(modalTaxRateFormSection, true);
        AdminModalController.toggle(modalDeleteMessageLabel, false);

        modalTaxRateCombo.getItems().setAll(viewModel.getAllTaxRates());
        modalTaxRateCombo.getSelectionModel().selectFirst();
        AdminTaxRateDTO selected = modalTaxRateCombo.getValue();
        viewModel.openEditModal(selected);
        populateFields(selected);
        return PersistResult.ok("Modal aberto.");
    }

    public PersistResult saveTaxRate() {
        try {
            AdminTaxRateDTO selected = modalTaxRateCombo.getValue();
            if (selected == null || selected.getId() == null) {
                modalController.showError("Selecione uma taxa de IVA valida.");
                return PersistResult.handledError();
            }

            BigDecimal rate = AdminParseUtils.parseRequiredDecimal(modalTaxRateValueField.getText(), "Taxa de IVA");
            viewModel.getAdminService().updateTaxRate(selected.getId(), new AdminTaxRateCommand(
                    modalTaxRateNameField.getText(),
                    rate,
                    modalTaxRateDescriptionField.getText(),
                    modalTaxRateActiveCheck.isSelected()));
            return PersistResult.ok("Taxa de IVA atualizada com sucesso.");
        } catch (Exception exception) {
            modalController.showError("Falha ao atualizar taxa de IVA: " + exception.getMessage());
            return PersistResult.handledError();
        }
    }

    public void close() {
        viewModel.closeModal();
        modalController.hide();
    }

    private void populateFields(AdminTaxRateDTO taxRate) {
        if (taxRate == null) {
            modalTaxRateNameField.clear();
            modalTaxRateValueField.clear();
            modalTaxRateDescriptionField.clear();
            modalTaxRateActiveCheck.setSelected(false);
            return;
        }

        modalTaxRateNameField
                .setText(AdminFormatUtils.fallback(taxRate.getName()).equals("-") ? "" : taxRate.getName());
        modalTaxRateValueField.setText(taxRate.getRate() == null ? "" : taxRate.getRate().toPlainString());
        modalTaxRateDescriptionField
                .setText(AdminFormatUtils.fallback(taxRate.getDescription()).equals("-") ? ""
                        : taxRate.getDescription());
        modalTaxRateActiveCheck.setSelected(Boolean.TRUE.equals(taxRate.getActive()));
    }

    public record PersistResult(boolean success, String message, boolean silent) {
        public static PersistResult ok(String message) {
            return new PersistResult(true, message, false);
        }

        public static PersistResult fail(String message) {
            return new PersistResult(false, message, false);
        }

        public static PersistResult handledError() {
            return new PersistResult(false, null, true);
        }
    }
}
