package com.obar.web.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    private final ClientAuthenticationInterceptor clientAuthenticationInterceptor;

    public WebMvcConfig(ClientAuthenticationInterceptor clientAuthenticationInterceptor) {
        this.clientAuthenticationInterceptor = clientAuthenticationInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(clientAuthenticationInterceptor)
                .addPathPatterns("/app/**");
    }
}
