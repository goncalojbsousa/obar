package com.obar.desktop.admin.sections.drivers;

import com.obar.desktop.admin.sections.users.AbstractUsersController;
import com.obar.model.enums.UserType;

public class DriversController extends AbstractUsersController {

    @Override
    protected UserType supportedType() {
        return UserType.DRIVER;
    }

    @Override
    protected String sectionTitle() {
        return "Motoristas";
    }

    @Override
    protected String createLabel() {
        return "+ Novo Motorista";
    }

    @Override
    protected String badgeLabel() {
        return "Motorista";
    }

    @Override
    protected String searchPrompt() {
        return "Pesquisar por nome, email, telefone, licenca...";
    }

    @Override
    protected String referenceTitle() {
        return "Licenca";
    }

    @Override
    protected String referencePrompt() {
        return "Numero de licenca";
    }

    @Override
    protected String metricTitle() {
        return "Avaliacao";
    }

    @Override
    protected String volumeTitle() {
        return "Viagens";
    }

    @Override
    protected String detailCardOneTitle() {
        return "Avaliacao";
    }

    @Override
    protected String detailCardTwoTitle() {
        return "Viagens";
    }

    @Override
    protected String detailCardThreeTitle() {
        return "Disponivel";
    }

    @Override
    protected String detailCardFourTitle() {
        return "Estado";
    }
}
