package com.obar.desktop.admin.sections.drivers;

import com.obar.bll.admin.AdminService;
import com.obar.bll.admin.AdminUserDTO;
import com.obar.model.enums.AccountStatus;

/**
 * Presenter responsible for driver approval and rejection actions.
 *
 * <p>Kept in a dedicated file to isolate the approval workflow from the
 * generic user-management logic in {@link AbstractUsersController}.</p>
 *
 * <p>Usage pattern:</p>
 * <pre>
 *   PersistResult result = approvalPresenter.approve(selected, "Aprovado via admin");
 *   if (result.success()) { ... }
 * </pre>
 */
public final class DriverApprovalPresenter {

    private final AdminService adminService;

    public DriverApprovalPresenter(AdminService adminService) {
        if (adminService == null) {
            throw new IllegalArgumentException("AdminService must not be null.");
        }
        this.adminService = adminService;
    }

    /**
     * Result of an approval/rejection action.
     *
     * @param success whether the operation succeeded
     * @param message feedback message for the UI
     */
    public record PersistResult(boolean success, String message) {}

    /**
     * Approves a pending driver.
     *
     * @param driver       the DTO of the driver to approve (must not be null, must be PENDING)
     * @param approvalNote optional internal note
     * @return result with success flag and UI message
     */
    public PersistResult approve(AdminUserDTO driver, String approvalNote) {
        if (driver == null) {
            return new PersistResult(false, "Selecione um motorista pendente primeiro.");
        }
        if (driver.getStatus() != AccountStatus.PENDING) {
            return new PersistResult(false, "Apenas motoristas com estado 'Pendente' podem ser aprovados.");
        }
        try {
            adminService.approveDriver(driver.getId(), approvalNote);
            return new PersistResult(true, "Motorista \"" + driver.getName() + "\" aprovado com sucesso.");
        } catch (IllegalArgumentException e) {
            return new PersistResult(false, e.getMessage());
        }
    }

    /**
     * Rejects a pending driver.
     *
     * @param driver         the DTO of the driver to reject (must not be null, must be PENDING)
     * @param rejectionNote  optional reason for rejection
     * @return result with success flag and UI message
     */
    public PersistResult reject(AdminUserDTO driver, String rejectionNote) {
        if (driver == null) {
            return new PersistResult(false, "Selecione um motorista pendente primeiro.");
        }
        if (driver.getStatus() != AccountStatus.PENDING) {
            return new PersistResult(false, "Apenas motoristas com estado 'Pendente' podem ser rejeitados.");
        }
        try {
            adminService.rejectDriver(driver.getId(), rejectionNote);
            return new PersistResult(true, "Motorista \"" + driver.getName() + "\" rejeitado.");
        } catch (IllegalArgumentException e) {
            return new PersistResult(false, e.getMessage());
        }
    }
}
