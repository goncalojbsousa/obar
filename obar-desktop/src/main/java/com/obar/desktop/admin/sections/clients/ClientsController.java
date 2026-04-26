package com.obar.desktop.admin.sections.clients;

import com.obar.desktop.admin.sections.users.AbstractUsersController;
import com.obar.desktop.admin.sections.users.UsersViewModel;

public class ClientsController extends AbstractUsersController {

    @Override
    protected UsersViewModel createViewModel() {
        return new ClientsViewModel();
    }
}
