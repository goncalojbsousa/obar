package com.obar.desktop.admin.sections.users;

import com.obar.bll.admin.AdminService;
import com.obar.bll.admin.AdminUserDTO;
import com.obar.desktop.admin.shared.AdminFormatUtils;
import com.obar.model.enums.AccountStatus;
import com.obar.model.enums.UserType;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;

/**
 * Abstract ViewModel for user admin sections (Drivers, Clients).
 *
 * <p>
 * Owns all mutable state: full user list, active filter, search query,
 * and modal mode. Subclasses declare what makes each section different
 * (user type, labels) - the same contract that was previously on the
 * controller.
 */
public abstract class UsersViewModel {

    // - backing data -
    private final ObservableList<AdminUserDTO> allUsers = FXCollections.observableArrayList();
    private final FilteredList<AdminUserDTO> filteredUsers = new FilteredList<>(allUsers, u -> true);

    // - filter state -
    private final ObjectProperty<AccountStatus> activeStatusFilter = new SimpleObjectProperty<>(null);
    private final StringProperty searchQuery = new SimpleStringProperty("");

    // - modal state -
    private ModalMode modalMode = ModalMode.NONE;
    private AdminUserDTO modalTarget = null;

    private AdminService adminService;

    /** Modes the modal can be in. Package-private so presenter can read it. */
    enum ModalMode {
        NONE, CREATE, EDIT, DELETE_CONFIRM
    }

    // ---------------------------------------------
    // Abstract section contract (replaces abstract methods on controller)
    // ---------------------------------------------

    public abstract UserType supportedType();

    public abstract String sectionTitle();

    public abstract String createLabel();

    public abstract String badgeLabel();

    public abstract String searchPrompt();

    public abstract String referenceTitle();

    public abstract String referencePrompt();

    public abstract String metricTitle();

    public abstract String volumeTitle();

    public abstract String detailCardOneTitle();

    public abstract String detailCardTwoTitle();

    public abstract String detailCardThreeTitle();

    public abstract String detailCardFourTitle();

    // ---------------------------------------------
    // Initialization
    // ---------------------------------------------

    public void init(AdminService adminService) {
        this.adminService = adminService;
        activeStatusFilter.addListener((obs, previous, current) -> applyPredicate());
        searchQuery.addListener((obs, previous, current) -> applyPredicate());
    }

    // ---------------------------------------------
    // Data operations
    // ---------------------------------------------

    public void reload() {
        if (adminService == null) {
            return;
        }
        allUsers.setAll(adminService.listUsersByType(supportedType()));
        modalTarget = null;
    }

    // ---------------------------------------------
    // Filter helpers
    // ---------------------------------------------

    public void setStatusFilter(AccountStatus status) {
        activeStatusFilter.set(status);
    }

    private void applyPredicate() {
        String normalizedQuery = AdminFormatUtils.normalize(searchQuery.get());
        AccountStatus filter = activeStatusFilter.get();
        filteredUsers.setPredicate(user -> matchesStatus(user, filter) && matchesQuery(user, normalizedQuery));
    }

    private boolean matchesStatus(AdminUserDTO user, AccountStatus filter) {
        return filter == null || user.getStatus() == filter;
    }

    private boolean matchesQuery(AdminUserDTO user, String query) {
        if (query.isBlank()) {
            return true;
        }
        return AdminFormatUtils.normalize(user.getName()).contains(query)
                || AdminFormatUtils.normalize(user.getEmail()).contains(query)
                || AdminFormatUtils.normalize(user.getPhone()).contains(query)
                || AdminFormatUtils.normalize(user.getLicenseNumber()).contains(query)
                || AdminFormatUtils.normalize(user.getTaxNumber()).contains(query)
                || AdminFormatUtils.normalize(AdminFormatUtils.prettyStatus(user.getStatus())).contains(query);
    }

    // ---------------------------------------------
    // Modal state
    // ---------------------------------------------

    public void openCreateModal() {
        modalMode = ModalMode.CREATE;
        modalTarget = null;
    }

    public void openEditModal(AdminUserDTO user) {
        modalMode = ModalMode.EDIT;
        modalTarget = user;
    }

    public void openDeleteModal(AdminUserDTO user) {
        modalMode = ModalMode.DELETE_CONFIRM;
        modalTarget = user;
    }

    public void closeModal() {
        modalMode = ModalMode.NONE;
        modalTarget = null;
    }

    // ---------------------------------------------
    // Column value helpers (type-aware, used by controller for cell factories)
    // ---------------------------------------------

    public String metricValue(AdminUserDTO user) {
        return supportedType() == UserType.DRIVER
                ? AdminFormatUtils.starRating(user.getAverageRating())
                : AdminFormatUtils.fallback(user.getEmail());
    }

    public String volumeValue(AdminUserDTO user) {
        return supportedType() == UserType.DRIVER
                ? String.valueOf(AdminFormatUtils.defaultInteger(user.getTotalTrips()))
                : AdminFormatUtils.fallback(user.getPhone());
    }

    public String referenceValue(AdminUserDTO user) {
        return supportedType() == UserType.DRIVER
                ? AdminFormatUtils.fallback(user.getLicenseNumber())
                : AdminFormatUtils.fallback(user.getTaxNumber());
    }

    public String cardOneValue(AdminUserDTO user) {
        return supportedType() == UserType.DRIVER
                ? AdminFormatUtils.starRating(user.getAverageRating())
                : AdminFormatUtils.prettyStatus(user.getStatus());
    }

    public String cardTwoValue(AdminUserDTO user) {
        return supportedType() == UserType.DRIVER
                ? String.valueOf(AdminFormatUtils.defaultInteger(user.getTotalTrips()))
                : AdminFormatUtils.fallback(user.getEmail());
    }

    public String cardThreeValue(AdminUserDTO user) {
        return supportedType() == UserType.DRIVER
                ? (Boolean.TRUE.equals(user.getAvailable()) ? "Online" : "Offline")
                : (user.getDefaultPaymentMethodId() == null ? "-" : "#" + user.getDefaultPaymentMethodId());
    }

    public String cardFourValue(AdminUserDTO user) {
        return supportedType() == UserType.DRIVER
                ? AdminFormatUtils.prettyStatus(user.getStatus())
                : "Cliente";
    }

    // ---------------------------------------------
    // Exposed properties
    // ---------------------------------------------

    public ObservableList<AdminUserDTO> getAllUsers() {
        return allUsers;
    }

    public FilteredList<AdminUserDTO> getFilteredUsers() {
        return filteredUsers;
    }

    public ObjectProperty<AccountStatus> activeStatusFilterProperty() {
        return activeStatusFilter;
    }

    public StringProperty searchQueryProperty() {
        return searchQuery;
    }

    public ModalMode getModalMode() {
        return modalMode;
    }

    public AdminUserDTO getModalTarget() {
        return modalTarget;
    }

    public AdminService getAdminService() {
        return adminService;
    }
}
