package com.obar.desktop.admin.shared;

import javafx.scene.control.Button;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Manages a set of filter chip buttons, keeping exactly one active at a time.
 *
 * <p>Usage:
 * <pre>
 *     chips = new FilterChipManager&lt;AccountStatus&gt;()
 *         .add(filterAllButton,      null)
 *         .add(filterActiveButton,   AccountStatus.ACTIVE)
 *         .add(filterInactiveButton, AccountStatus.INACTIVE);
 *
 *     chips.setActive(AccountStatus.ACTIVE);
 * </pre>
 */
public final class FilterChipManager<T> {

    private static final String BASE  = "filter-chip";
    private static final String ACTIVE = "filter-chip-active";

    private record Chip<T>(Button button, T value) {}

    private final List<Chip<T>> chips = new ArrayList<>();

    public FilterChipManager<T> add(Button button, T value) {
        if (button != null) {
            chips.add(new Chip<>(button, value));
        }
        return this;
    }

    public void setActive(T activeValue) {
        for (Chip<T> chip : chips) {
            boolean active = Objects.equals(chip.value(), activeValue);
            chip.button().getStyleClass().removeAll(BASE, ACTIVE);
            chip.button().getStyleClass().add(active ? ACTIVE : BASE);
        }
    }
}
