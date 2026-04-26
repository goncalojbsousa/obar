package com.obar.desktop.admin.shared;

import javafx.scene.control.Label;
import javafx.scene.layout.VBox;

/**
 * Binds a fixed set of detail-panel labels to a {@link DetailViewModel},
 * removing the repetitive setText boilerplate from every section controller.
 *
 * <p>Construct once in {@code initialize()} by passing all {@code @FXML} labels,
 * then call {@link #bind(DetailViewModel)} or {@link #clear(DetailViewModel)}
 * whenever the selection changes.
 */
public final class DetailPanelBinder {

    /**
     * Immutable snapshot of every field shown in the detail panel.
     * Use the nested {@link Builder} to construct instances.
     */
    public record DetailViewModel(
            String initials,
            String title,
            String name,
            String email,
            String status,
            String role,
            String phone,
            String created,
            String card1Title, String card1Value,
            String card2Title, String card2Value,
            String card3Title, String card3Value,
            String card4Title, String card4Value,
            String refTitle,   String refValue,
            String extra1Title, String extra1Value,
            String extra2Title, String extra2Value,
            String extra3Title, String extra3Value
    ) {
        /** Convenience builder so callers don't need a 22-arg constructor. */
        public static final class Builder {
            private String initials = "--", title = "", name = "-", email = "-";
            private String status = "-", role = "-", phone = "-", created = "-";
            private String card1Title = "", card1Value = "-";
            private String card2Title = "", card2Value = "-";
            private String card3Title = "", card3Value = "-";
            private String card4Title = "", card4Value = "-";
            private String refTitle = "",   refValue = "-";
            private String extra1Title = "", extra1Value = "-";
            private String extra2Title = "", extra2Value = "-";
            private String extra3Title = "", extra3Value = "-";

            public Builder initials(String v)      { initials = v;      return this; }
            public Builder title(String v)         { title = v;         return this; }
            public Builder name(String v)          { name = v;          return this; }
            public Builder email(String v)         { email = v;         return this; }
            public Builder status(String v)        { status = v;        return this; }
            public Builder role(String v)          { role = v;          return this; }
            public Builder phone(String v)         { phone = v;         return this; }
            public Builder created(String v)       { created = v;       return this; }
            public Builder card1(String t, String v) { card1Title = t; card1Value = v; return this; }
            public Builder card2(String t, String v) { card2Title = t; card2Value = v; return this; }
            public Builder card3(String t, String v) { card3Title = t; card3Value = v; return this; }
            public Builder card4(String t, String v) { card4Title = t; card4Value = v; return this; }
            public Builder ref(String t, String v)   { refTitle = t;   refValue = v;   return this; }
            public Builder extra1(String t, String v) { extra1Title = t; extra1Value = v; return this; }
            public Builder extra2(String t, String v) { extra2Title = t; extra2Value = v; return this; }
            public Builder extra3(String t, String v) { extra3Title = t; extra3Value = v; return this; }

            public DetailViewModel build() {
                return new DetailViewModel(
                        initials, title, name, email, status, role, phone, created,
                        card1Title, card1Value, card2Title, card2Value,
                        card3Title, card3Value, card4Title, card4Value,
                        refTitle, refValue,
                        extra1Title, extra1Value, extra2Title, extra2Value, extra3Title, extra3Value);
            }
        }
    }

    private final VBox panel;
    private final Label initialsLabel, titleLabel, nameLabel, emailLabel;
    private final Label statusLabel, roleLabel, phoneLabel, createdLabel;
    private final Label card1TitleLabel, card1ValueLabel;
    private final Label card2TitleLabel, card2ValueLabel;
    private final Label card3TitleLabel, card3ValueLabel;
    private final Label card4TitleLabel, card4ValueLabel;
    private final Label refTitleLabel, refValueLabel;
    private final Label extra1TitleLabel, extra1ValueLabel;
    private final Label extra2TitleLabel, extra2ValueLabel;
    private final Label extra3TitleLabel, extra3ValueLabel;

    public DetailPanelBinder(
            VBox panel,
            Label initialsLabel, Label titleLabel, Label nameLabel, Label emailLabel,
            Label statusLabel,   Label roleLabel,   Label phoneLabel, Label createdLabel,
            Label card1TitleLabel, Label card1ValueLabel,
            Label card2TitleLabel, Label card2ValueLabel,
            Label card3TitleLabel, Label card3ValueLabel,
            Label card4TitleLabel, Label card4ValueLabel,
            Label refTitleLabel,   Label refValueLabel,
            Label extra1TitleLabel, Label extra1ValueLabel,
            Label extra2TitleLabel, Label extra2ValueLabel,
            Label extra3TitleLabel, Label extra3ValueLabel) {

        this.panel           = panel;
        this.initialsLabel   = initialsLabel;
        this.titleLabel      = titleLabel;
        this.nameLabel       = nameLabel;
        this.emailLabel      = emailLabel;
        this.statusLabel     = statusLabel;
        this.roleLabel       = roleLabel;
        this.phoneLabel      = phoneLabel;
        this.createdLabel    = createdLabel;
        this.card1TitleLabel = card1TitleLabel;
        this.card1ValueLabel = card1ValueLabel;
        this.card2TitleLabel = card2TitleLabel;
        this.card2ValueLabel = card2ValueLabel;
        this.card3TitleLabel = card3TitleLabel;
        this.card3ValueLabel = card3ValueLabel;
        this.card4TitleLabel = card4TitleLabel;
        this.card4ValueLabel = card4ValueLabel;
        this.refTitleLabel   = refTitleLabel;
        this.refValueLabel   = refValueLabel;
        this.extra1TitleLabel = extra1TitleLabel;
        this.extra1ValueLabel = extra1ValueLabel;
        this.extra2TitleLabel = extra2TitleLabel;
        this.extra2ValueLabel = extra2ValueLabel;
        this.extra3TitleLabel = extra3TitleLabel;
        this.extra3ValueLabel = extra3ValueLabel;
    }

    /**
     * Populates all labels WITHOUT changing panel visibility.
     * Use this for overview/summary binds that should not open the panel.
     */
    public void bindSilent(DetailViewModel vm) {
        set(initialsLabel, vm.initials());
        set(titleLabel,    vm.title());
        set(nameLabel,     vm.name());
        set(emailLabel,    vm.email());
        set(statusLabel,   vm.status());
        set(roleLabel,     vm.role());
        set(phoneLabel,    vm.phone());
        set(createdLabel,  vm.created());
        set(card1TitleLabel, vm.card1Title()); set(card1ValueLabel, vm.card1Value());
        set(card2TitleLabel, vm.card2Title()); set(card2ValueLabel, vm.card2Value());
        set(card3TitleLabel, vm.card3Title()); set(card3ValueLabel, vm.card3Value());
        set(card4TitleLabel, vm.card4Title()); set(card4ValueLabel, vm.card4Value());
        set(refTitleLabel,   vm.refTitle());   set(refValueLabel,   vm.refValue());
        set(extra1TitleLabel, vm.extra1Title()); set(extra1ValueLabel, vm.extra1Value());
        set(extra2TitleLabel, vm.extra2Title()); set(extra2ValueLabel, vm.extra2Value());
        set(extra3TitleLabel, vm.extra3Title()); set(extra3ValueLabel, vm.extra3Value());
    }

    /** Populates all labels and makes the panel visible. */
    public void bind(DetailViewModel vm) {
        set(initialsLabel, vm.initials());
        set(titleLabel,    vm.title());
        set(nameLabel,     vm.name());
        set(emailLabel,    vm.email());
        set(statusLabel,   vm.status());
        set(roleLabel,     vm.role());
        set(phoneLabel,    vm.phone());
        set(createdLabel,  vm.created());
        set(card1TitleLabel, vm.card1Title()); set(card1ValueLabel, vm.card1Value());
        set(card2TitleLabel, vm.card2Title()); set(card2ValueLabel, vm.card2Value());
        set(card3TitleLabel, vm.card3Title()); set(card3ValueLabel, vm.card3Value());
        set(card4TitleLabel, vm.card4Title()); set(card4ValueLabel, vm.card4Value());
        set(refTitleLabel,   vm.refTitle());   set(refValueLabel,   vm.refValue());
        set(extra1TitleLabel, vm.extra1Title()); set(extra1ValueLabel, vm.extra1Value());
        set(extra2TitleLabel, vm.extra2Title()); set(extra2ValueLabel, vm.extra2Value());
        set(extra3TitleLabel, vm.extra3Title()); set(extra3ValueLabel, vm.extra3Value());
        setVisible(true);
    }

    private static void set(Label label, String value) {
        if (label != null) {
            label.setText(value == null ? "" : value);
        }
    }

    /** Populates with the given empty-state values and hides the panel. */
    public void clear(DetailViewModel emptyState) {
        bind(emptyState);
        setVisible(false);
    }

    public void setVisible(boolean visible) {
        if (panel == null) {
            return;
        }
        panel.setVisible(visible);
        panel.setManaged(visible);
    }
}
