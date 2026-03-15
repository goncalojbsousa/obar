package com.obar;

import com.obar.bll.UserService;
import com.obar.model.User;
import com.obar.model.enums.UserType;

public class TestCore {
    public static void main(String[] args) {
        UserService service = new UserService();

        User user = new User();
        user.setName("Teste");
        user.setEmail("teste@obar.pt");
        user.setPasswordHash("hash123");
        user.setType(UserType.CLIENT);

        User saved = service.register(user);
        System.out.println("Utilizador criado com ID: " + saved.getId());
    }
}