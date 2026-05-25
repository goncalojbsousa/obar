package com.obar.web.auth;

import com.obar.bll.auth.AuthService;
import com.obar.bll.auth.AuthenticatedUserDto;
import com.obar.bll.auth.AuthenticationException;
import com.obar.model.enums.UserType;
import com.obar.web.session.WebSessionHelper;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/")
    public String home(HttpSession session) {
        return WebSessionHelper.isLoggedIn(session) ? "redirect:/app" : "redirect:/login";
    }

    @GetMapping("/login")
    public String login(Model model, HttpSession session) {
        if (WebSessionHelper.isLoggedIn(session)) {
            return "redirect:/app";
        }
        model.addAttribute("loginForm", new LoginForm());
        return "auth/login";
    }

    @PostMapping("/login")
    public String authenticate(@ModelAttribute LoginForm loginForm, Model model, HttpSession session) {
        try {
            AuthenticatedUserDto authenticatedUser = authService.authenticate(
                    loginForm.getEmail(),
                    loginForm.getPassword());

            WebSessionHelper.login(session, authenticatedUser);
            return "redirect:/app";
        } catch (AuthenticationException exception) {
            model.addAttribute("error", exception.getMessage());
            model.addAttribute("loginForm", loginForm);
            return "auth/login";
        }
    }

    @GetMapping("/register")
    public String register(Model model, HttpSession session) {
        if (WebSessionHelper.isLoggedIn(session)) {
            return "redirect:/app";
        }
        model.addAttribute("registerForm", new RegisterForm());
        return "auth/register";
    }

    @PostMapping("/register")
    public String createAccount(@ModelAttribute RegisterForm registerForm, Model model, HttpSession session) {
        if (!safe(registerForm.getPassword()).equals(safe(registerForm.getConfirmPassword()))) {
            model.addAttribute("error", "As palavras-passe não coincidem.");
            model.addAttribute("registerForm", registerForm);
            return "auth/register";
        }

        try {
            AuthenticatedUserDto authenticatedUser = authService.register(
                    registerForm.getName(),
                    registerForm.getEmail(),
                    registerForm.getPassword(),
                    UserType.CLIENT,
                    blankToNull(registerForm.getPhone()),
                    blankToNull(registerForm.getTaxNumber()));

            WebSessionHelper.login(session, authenticatedUser);
            return "redirect:/app";
        } catch (IllegalArgumentException | AuthenticationException exception) {
            model.addAttribute("error", exception.getMessage());
            model.addAttribute("registerForm", registerForm);
            return "auth/register";
        }
    }

    @PostMapping("/logout")
    public String logout(HttpSession session) {
        WebSessionHelper.logout(session);
        return "redirect:/login?logout";
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private String blankToNull(String value) {
        String safeValue = value == null ? "" : value.trim();
        return safeValue.isBlank() ? null : safeValue;
    }
}
