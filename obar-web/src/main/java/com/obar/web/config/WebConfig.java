package com.obar.web.config;

import com.obar.bll.CoreLifecycleService;
import com.obar.bll.ReviewService;
import com.obar.bll.RouteService;
import com.obar.bll.TaxRateService;
import com.obar.bll.TripService;
import com.obar.bll.UserService;
import com.obar.bll.VehicleService;
import com.obar.bll.auth.AuthService;
import com.obar.web.session.WebSessionHelper;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    @Bean
    public AuthService authService() {
        return new AuthService();
    }

    @Bean
    public RouteService routeService() {
        return new RouteService();
    }

    @Bean
    public TaxRateService taxRateService() {
        return new TaxRateService();
    }

    @Bean
    public TripService tripService() {
        return new TripService();
    }

    @Bean
    public ReviewService reviewService() {
        return new ReviewService();
    }

    @Bean
    public UserService userService() {
        return new UserService();
    }

    @Bean
    public VehicleService vehicleService() {
        return new VehicleService();
    }

    @Bean
    @ConditionalOnProperty(name = "obar.core.lifecycle.enabled", havingValue = "true", matchIfMissing = true)
    public CoreLifecycleAdapter coreLifecycleAdapter() {
        return new CoreLifecycleAdapter(new CoreLifecycleService());
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authenticationInterceptor())
                .addPathPatterns("/app/**", "/api/**");
    }

    private HandlerInterceptor authenticationInterceptor() {
        return new HandlerInterceptor() {
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
        };
    }

    static final class CoreLifecycleAdapter implements InitializingBean, DisposableBean {

        private final CoreLifecycleService coreLifecycleService;

        private CoreLifecycleAdapter(CoreLifecycleService coreLifecycleService) {
            this.coreLifecycleService = coreLifecycleService;
        }

        @Override
        public void afterPropertiesSet() {
            coreLifecycleService.warmUp();
        }

        @Override
        public void destroy() {
            coreLifecycleService.shutdown();
        }
    }
}
