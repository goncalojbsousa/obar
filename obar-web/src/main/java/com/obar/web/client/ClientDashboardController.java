package com.obar.web.client;

import com.obar.bll.auth.AuthenticatedUserDto;
import com.obar.web.session.WebSessionHelper;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ClientDashboardController {

    @GetMapping("/app")
    public String dashboard(HttpSession session, Model model) {
        AuthenticatedUserDto currentUser = WebSessionHelper.getCurrentUser(session).orElseThrow();
        model.addAttribute("currentUser", currentUser);
        return "client/dashboard";
    }
}
