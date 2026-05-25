package com.obar.desktop.admin.sections.drivers;

import com.obar.bll.admin.AdminUserDTO;
import com.obar.model.User;
import com.obar.model.enums.AccountStatus;
import com.obar.model.enums.UserType;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class PendingDriversControllerTest {

    @Test
    public void searchMatchesPendingDriversByNameEmailPhoneAndLicense() {
        AdminUserDTO ana = createPendingDriver("Ana Silva", "ana@example.com", "910000001", "PT-ANA");
        AdminUserDTO bruno = createPendingDriver("Bruno Costa", "bruno@example.com", "920000002", "PT-BRUNO");

        assertTrue(PendingDriversController.matchesPendingDriverSearch(ana, ""));
        assertTrue(PendingDriversController.matchesPendingDriverSearch(bruno, "bruno"));
        assertTrue(PendingDriversController.matchesPendingDriverSearch(ana, "PT-ANA"));
        assertFalse(PendingDriversController.matchesPendingDriverSearch(ana, "bruno"));
    }

    private AdminUserDTO createPendingDriver(String name, String email, String phone, String licenseNumber) {
        User driver = new User();
        driver.setName(name);
        driver.setEmail(email);
        driver.setPhone(phone);
        driver.setLicenseNumber(licenseNumber);
        driver.setType(UserType.DRIVER);
        driver.setStatus(AccountStatus.PENDING);
        return AdminUserDTO.from(driver);
    }
}
