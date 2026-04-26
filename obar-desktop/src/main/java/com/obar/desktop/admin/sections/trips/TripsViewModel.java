package com.obar.desktop.admin.sections.trips;

import com.obar.bll.admin.AdminService;
import com.obar.bll.admin.AdminTripDTO;
import com.obar.model.enums.TripStatus;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;

/**
 * ViewModel for the Trips admin section.
 *
 * <p>
 * Owns all mutable UI state: the full trip list, the active filter,
 * the search query, and the currently selected trip. The controller
 * observes these properties and never holds state itself.
 */
public final class TripsViewModel {

    // - backing data -
    private final ObservableList<AdminTripDTO> allTrips = FXCollections.observableArrayList();
    private final FilteredList<AdminTripDTO> filteredTrips = new FilteredList<>(allTrips, t -> true);

    // - filter state -
    private final ObjectProperty<TripStatus> activeStatusFilter = new SimpleObjectProperty<>(null);
    private final StringProperty searchQuery = new SimpleStringProperty("");

    // - selection state -
    private final ObjectProperty<AdminTripDTO> selectedTrip = new SimpleObjectProperty<>(null);

    // - modal state -
    private ModalMode modalMode = ModalMode.NONE;
    private AdminTripDTO modalTarget = null;

    private AdminService adminService;

    /** Modes the modal can be in. Package-private so presenter can read it. */
    enum ModalMode {
        NONE, CREATE, EDIT, DELETE_CONFIRM
    }

    // ---------------------------------------------
    // Initialisation
    // ---------------------------------------------

    /**
     * Injects the service and wires the filter predicate to the observable
     * properties.
     * Must be called once before the controller calls {@link #reload()}.
     */
    public void init(AdminService adminService) {
        this.adminService = adminService;
        activeStatusFilter.addListener((obs, previous, current) -> applyPredicate());
        searchQuery.addListener((obs, previous, current) -> applyPredicate());
    }

    // ---------------------------------------------
    // Data operations
    // ---------------------------------------------

    /** Fetches fresh data from the service and resets selection. */
    public void reload() {
        if (adminService == null) {
            return;
        }
        allTrips.setAll(adminService.listTrips());
        selectedTrip.set(null);
    }

    // ---------------------------------------------
    // Filter helpers
    // ---------------------------------------------

    public void setStatusFilter(TripStatus status) {
        activeStatusFilter.set(status);
    }

    private void applyPredicate() {
        String normalizedQuery = normalize(searchQuery.get());
        TripStatus filter = activeStatusFilter.get();

        filteredTrips.setPredicate(trip -> matchesStatus(trip, filter) && matchesQuery(trip, normalizedQuery));
    }

    private boolean matchesStatus(AdminTripDTO trip, TripStatus filter) {
        return filter == null || trip.getStatus() == filter;
    }

    private boolean matchesQuery(AdminTripDTO trip, String query) {
        if (query.isBlank()) {
            return true;
        }
        String id = trip.getId() == null ? "" : String.valueOf(trip.getId());
        return normalize(id).contains(query)
                || normalize(trip.getClientName()).contains(query)
                || normalize(trip.getDriverName()).contains(query)
                || normalize(trip.getOriginAddress()).contains(query)
                || normalize(trip.getDestinationAddress()).contains(query);
    }

    private static String normalize(String value) {
        return value == null ? "" : value.trim().toLowerCase();
    }

    // ---------------------------------------------
    // Modal state
    // ---------------------------------------------

    public void openCreateModal() {
        modalMode = ModalMode.CREATE;
        modalTarget = null;
    }

    public void openEditModal(AdminTripDTO trip) {
        modalMode = ModalMode.EDIT;
        modalTarget = trip;
    }

    public void openDeleteModal(AdminTripDTO trip) {
        modalMode = ModalMode.DELETE_CONFIRM;
        modalTarget = trip;
    }

    public void closeModal() {
        modalMode = ModalMode.NONE;
        modalTarget = null;
    }

    // ---------------------------------------------
    // Exposed properties (read-only for controller)
    // ---------------------------------------------

    public ObservableList<AdminTripDTO> getAllTrips() {
        return allTrips;
    }

    public FilteredList<AdminTripDTO> getFilteredTrips() {
        return filteredTrips;
    }

    public ObjectProperty<TripStatus> activeStatusFilterProperty() {
        return activeStatusFilter;
    }

    public StringProperty searchQueryProperty() {
        return searchQuery;
    }

    public ObjectProperty<AdminTripDTO> selectedTripProperty() {
        return selectedTrip;
    }

    public ModalMode getModalMode() {
        return modalMode;
    }

    public AdminTripDTO getModalTarget() {
        return modalTarget;
    }

    public AdminService getAdminService() {
        return adminService;
    }
}
