package com.obar.desktop.admin.sections.financial;

import com.obar.bll.admin.AdminFinancialOverviewDTO;
import com.obar.bll.admin.AdminFinancialPeriod;
import com.obar.bll.admin.AdminPaymentByTripDTO;
import com.obar.bll.admin.AdminService;
import com.obar.bll.admin.AdminTaxRateDTO;
import com.obar.desktop.admin.shared.AdminFormatUtils;
import com.obar.model.enums.PaymentStatus;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.property.StringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * ViewModel for the Financial admin section.
 *
 * <p>
 * Owns all mutable state: payments, tax rates, financial overview,
 * active period, active payment status filter, search query, and modal mode.
 */
public final class FinancialViewModel {

    // - backing data -
    private final ObservableList<AdminPaymentByTripDTO> allPayments = FXCollections.observableArrayList();
    private final FilteredList<AdminPaymentByTripDTO> filteredPayments = new FilteredList<>(allPayments, p -> true);
    private final ObservableList<AdminTaxRateDTO> allTaxRates = FXCollections.observableArrayList();

    // - filter state -
    private final ObjectProperty<AdminFinancialPeriod> activePeriod = new SimpleObjectProperty<>(
            AdminFinancialPeriod.MONTH);
    private final ObjectProperty<PaymentStatus> activePaymentStatusFilter = new SimpleObjectProperty<>(null);
    private final StringProperty searchQuery = new SimpleStringProperty("");

    // - overview (refreshed on each reload) -
    private AdminFinancialOverviewDTO currentOverview;

    // - modal state (only EDIT for tax rate) -
    private boolean modalOpen = false;
    private AdminTaxRateDTO modalTargetTaxRate = null;

    private AdminService adminService;

    // ---------------------------------------------
    // Initialisation
    // ---------------------------------------------

    public void init(AdminService adminService) {
        this.adminService = adminService;
        activePeriod.addListener((obs, previous, current) -> reload());
        activePaymentStatusFilter.addListener((obs, previous, current) -> applyPredicate());
        searchQuery.addListener((obs, previous, current) -> applyPredicate());
    }

    // ---------------------------------------------
    // Data operations
    // ---------------------------------------------

    public void reload() {
        if (adminService == null) {
            return;
        }
        AdminFinancialPeriod period = activePeriod.get();
        allPayments.setAll(adminService.listPaymentsByTrip(period));
        allTaxRates.setAll(adminService.listTaxRates());
        currentOverview = adminService.getFinancialOverview(period);
        applyPredicate();
    }

    // ---------------------------------------------
    // Filter helpers
    // ---------------------------------------------

    public void setPeriod(AdminFinancialPeriod period) {
        // Listener on activePeriod triggers reload automatically
        if (period != null && period != activePeriod.get()) {
            activePeriod.set(period);
        }
    }

    public void setPaymentStatusFilter(PaymentStatus status) {
        activePaymentStatusFilter.set(status);
    }

    private void applyPredicate() {
        String query = AdminFormatUtils.normalize(searchQuery.get());
        PaymentStatus filter = activePaymentStatusFilter.get();
        filteredPayments.setPredicate(p -> matchesStatus(p, filter) && matchesQuery(p, query));
    }

    private boolean matchesStatus(AdminPaymentByTripDTO payment, PaymentStatus filter) {
        return filter == null || payment.getStatus() == filter;
    }

    private boolean matchesQuery(AdminPaymentByTripDTO payment, String query) {
        if (query.isBlank()) {
            return true;
        }
        String paymentId = payment.getPaymentId() == null ? "" : String.valueOf(payment.getPaymentId());
        String tripId = payment.getTripId() == null ? "" : String.valueOf(payment.getTripId());
        return AdminFormatUtils.normalize(paymentId).contains(query)
                || AdminFormatUtils.normalize(tripId).contains(query)
                || AdminFormatUtils.normalize(payment.getClientName()).contains(query)
                || AdminFormatUtils.normalize(payment.getDriverName()).contains(query)
                || AdminFormatUtils.normalize(AdminFormatUtils.prettyPaymentMethod(payment.getPaymentMethodType()))
                        .contains(query)
                || AdminFormatUtils.normalize(AdminFormatUtils.prettyPaymentStatus(payment.getStatus()))
                        .contains(query);
    }

    // ---------------------------------------------
    // Derived aggregates
    // ---------------------------------------------

    /** Groups all payments by prettified payment method name → count. */
    public Map<String, Long> groupPaymentMethods() {
        Map<String, Long> grouped = new HashMap<>();
        for (AdminPaymentByTripDTO p : allPayments) {
            String key = AdminFormatUtils.normalize(AdminFormatUtils.prettyPaymentMethod(p.getPaymentMethodType()));
            grouped.put(key, grouped.getOrDefault(key, 0L) + 1);
        }
        return grouped;
    }

    /**
     * Count of distinct drivers with at least one PROCESSED payment in current
     * period.
     */
    public long countPaidDrivers() {
        return allPayments.stream()
                .filter(p -> p.getStatus() == PaymentStatus.PROCESSED)
                .map(p -> AdminFormatUtils.normalize(p.getDriverName()))
                .filter(name -> !name.isBlank())
                .distinct()
                .count();
    }

    // ---------------------------------------------
    // Modal state
    // ---------------------------------------------

    public void openEditModal(AdminTaxRateDTO taxRate) {
        modalOpen = true;
        modalTargetTaxRate = taxRate;
    }

    public void closeModal() {
        modalOpen = false;
        modalTargetTaxRate = null;
    }

    public boolean isModalOpen() {
        return modalOpen;
    }

    public AdminTaxRateDTO getModalTargetTaxRate() {
        return modalTargetTaxRate;
    }

    // ---------------------------------------------
    // Exposed properties
    // ---------------------------------------------

    public ObservableList<AdminPaymentByTripDTO> getAllPayments() {
        return allPayments;
    }

    public FilteredList<AdminPaymentByTripDTO> getFilteredPayments() {
        return filteredPayments;
    }

    public ObservableList<AdminTaxRateDTO> getAllTaxRates() {
        return allTaxRates;
    }

    public ObjectProperty<AdminFinancialPeriod> activePeriodProperty() {
        return activePeriod;
    }

    public AdminFinancialPeriod getActivePeriod() {
        return activePeriod.get();
    }

    public ObjectProperty<PaymentStatus> activePaymentStatusFilterProperty() {
        return activePaymentStatusFilter;
    }

    public StringProperty searchQueryProperty() {
        return searchQuery;
    }

    public AdminFinancialOverviewDTO getCurrentOverview() {
        return currentOverview;
    }

    public AdminService getAdminService() {
        return adminService;
    }

    /** Snapshot of all payments for export (immutable copy). */
    public List<AdminPaymentByTripDTO> getPaymentsSnapshot() {
        return List.copyOf(allPayments);
    }

    /** Snapshot of all tax rates for export (immutable copy). */
    public List<AdminTaxRateDTO> getTaxRatesSnapshot() {
        return List.copyOf(allTaxRates);
    }
}
