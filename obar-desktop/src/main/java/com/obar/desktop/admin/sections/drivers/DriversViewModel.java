package com.obar.desktop.admin.sections.drivers;

import com.obar.desktop.admin.sections.users.UsersViewModel;
import com.obar.model.enums.UserType;

/**
 * ViewModel for the Drivers admin section.
 * Provides driver-specific labels and user type to the shared base.
 */
public final class DriversViewModel extends UsersViewModel {

    @Override
    public UserType supportedType() {
        return UserType.DRIVER;
    }

    @Override
    public String sectionTitle() {
        return "Motoristas";
    }

    @Override
    public String createLabel() {
        return "+ Novo Motorista";
    }

    @Override
    public String badgeLabel() {
        return "Motorista";
    }

    @Override
    public String searchPrompt() {
        return "Pesquisar por nome, email, telefone, licenca...";
    }

    @Override
    public String referenceTitle() {
        return "Licenca";
    }

    @Override
    public String referencePrompt() {
        return "Numero de licenca";
    }

    @Override
    public String metricTitle() {
        return "Avaliacao";
    }

    @Override
    public String volumeTitle() {
        return "Viagens";
    }

    @Override
    public String detailCardOneTitle() {
        return "Avaliacao";
    }

    @Override
    public String detailCardTwoTitle() {
        return "Viagens";
    }

    @Override
    public String detailCardThreeTitle() {
        return "Disponivel";
    }

    @Override
    public String detailCardFourTitle() {
        return "Estado";
    }
}
