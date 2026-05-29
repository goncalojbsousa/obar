package com.obar.web.config;

import com.obar.web.session.WebSessionHelper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
public class ClientAuthenticationInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws Exception {
        if (WebSessionHelper.isLoggedIn(request.getSession(false))) {
            return true;
        }

        if (request.getRequestURI().startsWith(request.getContextPath() + "/api/")) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
            return false;
        }

        response.sendRedirect(request.getContextPath() + "/login");
        return false;
    }
}
