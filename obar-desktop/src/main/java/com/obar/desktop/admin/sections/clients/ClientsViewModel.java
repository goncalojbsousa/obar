package com.obar.desktop.admin.sections.clients;

import com.obar.desktop.admin.sections.users.UsersViewModel;
import com.obar.model.enums.UserType;

/**
 * ViewModel for the Clients admin section.
 * Provides client-specific labels and user type to the shared base.
 */
public final class ClientsViewModel extends UsersViewModel {

    @Override
    public UserType supportedType() {
        return UserType.CLIENT;
    }

    @Override
    public String sectionTitle() {
        return "Clientes";
    }

    @Override
    public String createLabel() {
        return "+ Novo Cliente";
    }

    @Override
    public String badgeLabel() {
        return "Cliente";
    }

    @Override
    public String searchPrompt() {
        return "Pesquisar por nome, email, telefone, NIF...";
    }

    @Override
    public String referenceTitle() {
        return "NIF";
    }

    @Override
    public String referencePrompt() {
        return "NIF";
    }

    @Override
    public String metricTitle() {
        return "Email";
    }

    @Override
    public String volumeTitle() {
        return "Telefone";
    }

    @Override
    public String detailCardOneTitle() {
        return "Estado";
    }

    @Override
    public String detailCardTwoTitle() {
        return "Email";
    }

    @Override
    public String detailCardThreeTitle() {
        return "Metodo padrao";
    }

    @Override
    public String detailCardFourTitle() {
        return "Conta";
    }
}
