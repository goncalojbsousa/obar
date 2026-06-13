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
        return WebSessionHelper.getCurrentUser(session)
                .map(this::redirectFor)
                .orElse("redirect:/login");
    }

    @GetMapping("/login")
    public String login(Model model, HttpSession session) {
        var currentUser = WebSessionHelper.getCurrentUser(session);
        if (currentUser.isPresent()) {
            return redirectFor(currentUser.get());
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
            return redirectFor(authenticatedUser);
        } catch (AuthenticationException exception) {
            model.addAttribute("error", exception.getMessage());
            model.addAttribute("loginForm", loginForm);
            return "auth/login";
        }
    }

    @GetMapping("/register")
    public String register(Model model, HttpSession session) {
        var currentUser = WebSessionHelper.getCurrentUser(session);
        if (currentUser.isPresent()) {
            return redirectFor(currentUser.get());
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

    private String redirectFor(AuthenticatedUserDto user) {
        return user.type() == UserType.DRIVER ? "redirect:/driver" : "redirect:/app";
    }

    public static class LoginForm {

        private String email;
        private String password;

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }
    }

    public static class RegisterForm {

        private String name;
        private String email;
        private String phone;
        private String taxNumber;
        private String password;
        private String confirmPassword;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getEmail() {
            return email;
        }

        public void setEmail(String email) {
            this.email = email;
        }

        public String getPhone() {
            return phone;
        }

        public void setPhone(String phone) {
            this.phone = phone;
        }

        public String getTaxNumber() {
            return taxNumber;
        }

        public void setTaxNumber(String taxNumber) {
            this.taxNumber = taxNumber;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        public String getConfirmPassword() {
            return confirmPassword;
        }

        public void setConfirmPassword(String confirmPassword) {
            this.confirmPassword = confirmPassword;
        }
    }
}
