package de.jklein.pharmalink.config;

import de.jklein.pharmalink.service.auth.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;

import jakarta.servlet.http.HttpServletResponse;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;

    public SecurityConfig(CustomUserDetailsService customUserDetailsService) {
        this.customUserDetailsService = customUserDetailsService;
    }

    /**
     * Definiert den Passwort-Encoder als Bean. Verwendet BCrypt.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    /**
     * Konfiguriert den AuthenticationProvider, der den CustomUserDetailsService
     * und den PasswordEncoder für die Authentifizierung verwendet.
     */
    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(customUserDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    /**
     * Sicherheitskonfiguration für die stateless REST-API (/api/**).
     * @Order(1) sorgt dafür, dass diese Kette vor der allgemeineren Web-Konfiguration geprüft wird.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/**") // Diese Kette nur für Pfade unter /api/ anwenden
                .authorizeHttpRequests(authorize -> authorize
                        .anyRequest().authenticated() // Alle API-Anfragen erfordern Authentifizierung
                )
                .httpBasic(Customizer.withDefaults()) // HTTP Basic Authentifizierung für die API aktivieren
                .csrf(AbstractHttpConfigurer::disable) // CSRF für stateless APIs deaktivieren
                .exceptionHandling(exceptions -> exceptions
                        // Bei Fehlern eine JSON-Antwort senden, statt einer HTML-Seite
                        .authenticationEntryPoint(getApiAuthenticationEntryPoint())
                );

        return http.build();
    }

    /**
     * Sicherheitskonfiguration für die stateful Webanwendung (alle anderen Pfade).
     * Sichert die Anwendung mit einem Formular-Login.
     */
    @Bean
    @Order(2)
    public SecurityFilterChain formLoginFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        // Statische Ressourcen und öffentliche Seiten für alle freigeben
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()
                        .requestMatchers("/error", "/app/login", "/").permitAll()
                        // Der Endpunkt für die Authentifizierung muss ebenfalls frei sein
                        .requestMatchers(HttpMethod.POST, "/authenticate").permitAll()
                        // Alle anderen Anfragen müssen authentifiziert sein
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/app/login")           // URL zur Login-Seite
                        .loginProcessingUrl("/authenticate") // URL, an die das Login-Formular gesendet wird
                        .defaultSuccessUrl("/app/dashboard", true) // Ziel nach erfolgreichem Login
                        .failureUrl("/app/login?error")     // Ziel bei fehlerhaftem Login
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/app/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                );

        return http.build();
    }

    /**
     * Definiert den AuthenticationEntryPoint für die API, der einen 401-Status und eine JSON-Fehlermeldung zurückgibt.
     */
    private AuthenticationEntryPoint getApiAuthenticationEntryPoint() {
        return (request, response, authException) -> {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            response.getWriter().write(
                    String.format("{ \"error\": \"Unauthorized\", \"message\": \"%s\" }", authException.getMessage())
            );
        };
    }
}