package com.obar.web.config;

import com.obar.bll.CoreLifecycleService;
import com.obar.bll.RouteService;
import com.obar.bll.TripService;
import com.obar.bll.UserService;
import com.obar.bll.auth.AuthService;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebCoreConfig {

    @Bean
    public AuthService authService() {
        return new AuthService();
    }

    @Bean
    public RouteService routeService() {
        return new RouteService();
    }

    @Bean
    public TripService tripService() {
        return new TripService();
    }

    @Bean
    public UserService userService() {
        return new UserService();
    }

    @Bean
    @ConditionalOnProperty(name = "obar.core.lifecycle.enabled", havingValue = "true", matchIfMissing = true)
    public CoreLifecycleAdapter coreLifecycleAdapter() {
        return new CoreLifecycleAdapter(new CoreLifecycleService());
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
