package com.obar.web.driver;

import com.obar.bll.auth.AuthenticatedUserDto;
import com.obar.model.enums.UserType;
import com.obar.web.session.WebSessionHelper;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.server.ResponseStatusException;

@Controller
public class DriverDashboardController {

    @GetMapping("/driver")
    public String dashboard(HttpSession session, Model model) {
        AuthenticatedUserDto currentUser = WebSessionHelper.getCurrentUser(session).orElseThrow();
        if (currentUser.type() != UserType.DRIVER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Área reservada a motoristas.");
        }

        model.addAttribute("currentUser", currentUser);
        return "driver/dashboard";
    }
}
