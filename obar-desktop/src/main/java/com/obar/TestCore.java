package com.obar;

import com.obar.bll.auth.AuthService;
import com.obar.bll.auth.AuthenticatedUserDto;
import com.obar.model.enums.UserType;

public class TestCore {
    public static void main(String[] args) {
        AuthService authService = new AuthService();
        AuthenticatedUserDto saved = authService.register("Teste", "teste@obar.pt", "Pass1234!", UserType.CLIENT, null, null);
        System.out.println("User created with ID: " + saved.id());
    }
}