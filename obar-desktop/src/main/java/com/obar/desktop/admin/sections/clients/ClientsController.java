package com.obar.desktop.admin.sections.clients;

import com.obar.desktop.admin.sections.users.UsersController;
import com.obar.model.enums.UserType;

/**
 * Controller for the Clients admin section.
 *
 * <p>
 * Provides the client-specific labels and field names used by
 * {@link UsersController}.
 * </p>
 */
public class ClientsController extends UsersController {

    private static final UserSectionConfig CONFIG = new UserSectionConfig(
            UserType.CLIENT,
            "Clientes",
            "+ Novo Cliente",
            "Cliente",
            "Pesquisar por nome, email, telefone, NIF...",
            "NIF",
            "NIF",
            "Email",
            "Telefone",
            "Estado",
            "Email",
            "Metodo padrao",
            "Conta");

    @Override
    protected UserSectionConfig sectionConfig() {
        return CONFIG;
    }
}
