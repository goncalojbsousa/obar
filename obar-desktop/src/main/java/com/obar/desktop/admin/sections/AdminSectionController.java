package com.obar.desktop.admin.sections;

import com.obar.bll.admin.AdminService;

/**
 * Contract used by the admin page orchestrator to notify included section controllers.
 */
public interface AdminSectionController {

    /**
     * Injects AdminService used by each section.
     */
    void setAdminService(AdminService adminService);

    /**
     * Called whenever the section becomes active in the admin page.
     */
    void onSectionActivated();
}
