package de.jklein.pharmalink.config;

import de.jklein.pharmalink.api.audit.ApiTransactionInterceptor;
import de.jklein.pharmalink.filter.RequestCachingFilter; // NEU: Import Ihres benutzerdefinierten Filters
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {

    private final ApiTransactionInterceptor apiTransactionInterceptor;
    @Autowired
    public WebConfig(ApiTransactionInterceptor apiTransactionInterceptor, RequestCachingFilter requestCachingFilter) { // NEU: Filter im Konstruktor hinzufügen
        this.apiTransactionInterceptor = apiTransactionInterceptor;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(apiTransactionInterceptor).addPathPatterns("/api/v1/**");
    }
}