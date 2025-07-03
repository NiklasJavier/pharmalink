package de.jklein.pharmalink.config;

import de.jklein.pharmalink.service.auth.CustomUserDetailsService;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;

    public SecurityConfig(CustomUserDetailsService customUserDetailsService) {
        this.customUserDetailsService = customUserDetailsService;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(customUserDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        // Für statische Ressourcen und öffentlich zugängliche Seiten:
                        // requestMatchers kann Ant-Style Pfadmuster direkt als String verarbeiten.
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/webjars/**", "/favicon.ico").permitAll()
                        .requestMatchers("/app/login", "/").permitAll()
                        // Für den Login-Processing-Endpunkt:
                        // explizite Angabe der HTTP-Methode POST und des Pfades als String.
                        .requestMatchers(org.springframework.http.HttpMethod.POST, "/authenticate").permitAll()
                        // Alle anderen Anfragen müssen authentifiziert sein.
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/app/login") // URL Ihrer benutzerdefinierten Login-Seite
                        .loginProcessingUrl("/authenticate") // URL, an die das Login-Formular POSTet
                        .defaultSuccessUrl("/app/dashboard", true) // Umleitung nach erfolgreichem Login
                        .failureUrl("/app/login?error") // Umleitung bei fehlgeschlagenem Login
                        .permitAll()
                )
                .logout(logout -> logout
                        // Für den Logout-Endpunkt:
                        // logoutUrl() ist der empfohlene Weg und benötigt keinen AntPathRequestMatcher.
                        .logoutUrl("/logout") // URL für den Logout-Request
                        .logoutSuccessUrl("/app/login?logout") // URL nach erfolgreichem Logout
                        .permitAll()
                );

        return http.build();
    }
}