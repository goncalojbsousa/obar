package com.obar.desktop.admin.shared;

import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

/**
 * Reusable controller for common admin modal behavior.
 */
public final class AdminModalController {

    private final StackPane modalOverlay;
    private final Label modalTitleLabel;
    private final Label modalErrorLabel;
    private final VBox modalDeleteSection;
    private final Button modalSaveButton;
    private final Button modalDeleteConfirmButton;

    public AdminModalController(
            StackPane modalOverlay,
            Label modalTitleLabel,
            Label modalErrorLabel,
            VBox modalDeleteSection,
            Button modalSaveButton,
            Button modalDeleteConfirmButton) {
        this.modalOverlay = modalOverlay;
        this.modalTitleLabel = modalTitleLabel;
        this.modalErrorLabel = modalErrorLabel;
        this.modalDeleteSection = modalDeleteSection;
        this.modalSaveButton = modalSaveButton;
        this.modalDeleteConfirmButton = modalDeleteConfirmButton;
    }

    public void prepareForForm(String title) {
        setTitle(title);
        toggle(modalDeleteSection, false);
        toggle(modalSaveButton, true);
        toggle(modalDeleteConfirmButton, false);
        clearError();
        show();
    }

    public void prepareForDeleteConfirm(String title) {
        setTitle(title);
        toggle(modalDeleteSection, true);
        toggle(modalSaveButton, false);
        toggle(modalDeleteConfirmButton, true);
        clearError();
        show();
    }

    public void show() {
        toggle(modalOverlay, true);
    }

    public void hide() {
        toggle(modalOverlay, false);
        clearError();
    }

    public void setTitle(String title) {
        if (modalTitleLabel == null) {
            return;
        }
        modalTitleLabel.setText(title == null ? "" : title);
    }

    public void clearError() {
        if (modalErrorLabel == null) {
            return;
        }
        modalErrorLabel.setText("");
        toggle(modalErrorLabel, false);
    }

    public void showError(String message) {
        if (modalErrorLabel == null) {
            return;
        }
        modalErrorLabel.setText(message == null ? "" : message);
        toggle(modalErrorLabel, true);
    }

    public static void toggle(Node node, boolean visible) {
        if (node == null) {
            return;
        }
        node.setVisible(visible);
        node.setManaged(visible);
    }
}