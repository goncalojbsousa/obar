package com.obar.desktop.admin.sections.drivers.presenter;

import com.obar.bll.admin.AdminService;
import com.obar.bll.admin.AdminUserDTO;
import com.obar.model.enums.AccountStatus;

public final class DriverApprovalPresenter {
    private final AdminService adminService;

    public DriverApprovalPresenter(AdminService adminService) {
        if (adminService == null) {
            throw new IllegalArgumentException("AdminService must not be null.");
        }
        this.adminService = adminService;
    }

    public record PersistResult(boolean success, String message) {
    }

    public PersistResult approve(AdminUserDTO driver, String approvalNote) {
        if (driver == null)
            return new PersistResult(false, "Selecione um motorista pendente primeiro.");
        if (driver.getStatus() != AccountStatus.PENDING)
            return new PersistResult(false, "Apenas motoristas com estado 'Pendente' podem ser aprovados.");
        try {
            adminService.approveDriver(driver.getId(), approvalNote);
            return new PersistResult(true, "Motorista \"" + driver.getName() + "\" aprovado com sucesso.");
        } catch (IllegalArgumentException e) {
            return new PersistResult(false, e.getMessage());
        }
    }

    public PersistResult reject(AdminUserDTO driver, String rejectionNote) {
        if (driver == null)
            return new PersistResult(false, "Selecione um motorista pendente primeiro.");
        if (driver.getStatus() != AccountStatus.PENDING)
            return new PersistResult(false, "Apenas motoristas com estado 'Pendente' podem ser rejeitados.");
        try {
            adminService.rejectDriver(driver.getId(), rejectionNote);
            return new PersistResult(true, "Motorista \"" + driver.getName() + "\" rejeitado.");
        } catch (IllegalArgumentException e) {
            return new PersistResult(false, e.getMessage());
        }
    }
}
