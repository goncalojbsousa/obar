package com.obar;

import com.obar.bll.auth.AuthService;
import com.obar.bll.auth.AuthenticatedUserDto;
import com.obar.bll.auth.PasswordService;
import com.obar.dal.UserRepository;
import com.obar.model.enums.UserType;

public class TestCore {
    public static void main(String[] args) {
        AuthService authService = new AuthService(new UserRepository(), new PasswordService());
        AuthenticatedUserDto saved = authService.register("Teste", "teste@obar.pt", "Pass1234!", UserType.CLIENT);
        System.out.println("User created with ID: " + saved.id());
    }
}