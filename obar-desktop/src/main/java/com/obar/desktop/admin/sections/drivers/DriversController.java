package com.obar.desktop.admin.sections.drivers;

import com.obar.desktop.admin.sections.users.AbstractUsersController;
import com.obar.desktop.admin.sections.users.UsersViewModel;

public class DriversController extends AbstractUsersController {

    @Override
    protected UsersViewModel createViewModel() {
        return new DriversViewModel();
    }
}
