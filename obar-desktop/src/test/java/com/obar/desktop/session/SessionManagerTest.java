package com.obar.desktop.session;

import com.obar.bll.auth.AuthenticatedUserDto;
import com.obar.model.enums.AccountStatus;
import com.obar.model.enums.UserType;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class SessionManagerTest {

    @Test
    public void loginStoresCurrentUserAndLogoutClearsSession() {
        AuthenticatedUserDto adminUser = new AuthenticatedUserDto(
                1,
                "Admin User",
                "admin@obar.pt",
                UserType.ADMIN,
                AccountStatus.ACTIVE);

        SessionManager.login(adminUser);

        assertTrue(SessionManager.isLoggedIn());
        assertTrue(SessionManager.hasRole(UserType.ADMIN));
        assertEquals("Admin User", SessionManager.getCurrentUser().name());

        SessionManager.logout();

        assertFalse(SessionManager.isLoggedIn());
        assertFalse(SessionManager.hasRole(UserType.ADMIN));
    }
}
