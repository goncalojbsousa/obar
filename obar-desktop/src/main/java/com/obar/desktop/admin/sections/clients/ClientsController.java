package com.obar.desktop.admin.sections.clients;

import com.obar.desktop.admin.sections.users.AbstractUsersController;
import com.obar.model.enums.UserType;

public class ClientsController extends AbstractUsersController {

    @Override
    protected UserType supportedType() {
        return UserType.CLIENT;
    }

    @Override
    protected String sectionTitle() {
        return "Clientes";
    }

    @Override
    protected String createLabel() {
        return "+ Novo Cliente";
    }

    @Override
    protected String badgeLabel() {
        return "Cliente";
    }

    @Override
    protected String searchPrompt() {
        return "Pesquisar por nome, email, telefone, NIF...";
    }

    @Override
    protected String referenceTitle() {
        return "NIF";
    }

    @Override
    protected String referencePrompt() {
        return "NIF";
    }

    @Override
    protected String metricTitle() {
        return "Email";
    }

    @Override
    protected String volumeTitle() {
        return "Telefone";
    }

    @Override
    protected String detailCardOneTitle() {
        return "Estado";
    }

    @Override
    protected String detailCardTwoTitle() {
        return "Email";
    }

    @Override
    protected String detailCardThreeTitle() {
        return "Metodo padrao";
    }

    @Override
    protected String detailCardFourTitle() {
        return "Conta";
    }
}
