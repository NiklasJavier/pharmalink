package de.jklein.pharmalink.config;

import de.jklein.pharmalink.filter.auth.JwtRequestFilter;
import de.jklein.pharmalink.service.auth.CustomUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager; // Import hinzufügen
import org.springframework.security.authentication.ProviderManager; // Import hinzufügen
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy; // Import hinzufügen
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter; // Import hinzufügen
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import jakarta.servlet.http.HttpServletResponse;
import java.util.Arrays;
import java.util.Collections;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;
    private final JwtRequestFilter jwtRequestFilter; // JwtRequestFilter injizieren

    // Im Konstruktor nun auch JwtRequestFilter injizieren
    public SecurityConfig(CustomUserDetailsService customUserDetailsService, JwtRequestFilter jwtRequestFilter) {
        this.customUserDetailsService = customUserDetailsService;
        this.jwtRequestFilter = jwtRequestFilter;
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
     * NEU: Definiert den AuthenticationManager als Bean.
     * Dieser Manager wird vom AuthController verwendet, um die Authentifizierung durchzuführen.
     */
    @Bean
    public AuthenticationManager authenticationManager() {
        // Der ProviderManager kann einen oder mehrere AuthenticationProvider verwalten.
        return new ProviderManager(authenticationProvider());
    }


    /**
     * Sicherheitskonfiguration für die stateless REST-API (/api/**).
     * @Order(1) sorgt dafür, dass diese Kette vor der allgemeineren Web-Konfiguration geprüft wird.
     * Hier wird der JwtRequestFilter integriert.
     */
    @Bean
    @Order(1)
    public SecurityFilterChain apiSecurityFilterChain(HttpSecurity http) throws Exception {
        http
                .securityMatcher("/api/**") // Diese Kette nur für Pfade unter /api/ anwenden
                .csrf(AbstractHttpConfigurer::disable) // CSRF für stateless APIs deaktivieren
                .sessionManagement(session -> session
                        .sessionCreationPolicy(SessionCreationPolicy.STATELESS) // Wichtig für JWT-basierte APIs
                )
                .authorizeHttpRequests(authorize -> authorize
                        // Öffentliche Endpunkte für Authentifizierung und Swagger/API-Dokus
                        .requestMatchers("/api/v1/auth/**", "/swagger-ui/**", "/v3/api-docs/**", "/api/api-docs/**").permitAll()
                        .anyRequest().authenticated() // Alle anderen API-Anfragen erfordern Authentifizierung
                )
                // Hinzufügen des JWT-Filters vor dem Standard-Authentifizierungsfilter
                .addFilterBefore(jwtRequestFilter, UsernamePasswordAuthenticationFilter.class)
                .exceptionHandling(exceptions -> exceptions
                        // Bei Authentifizierungsfehlern für die API eine JSON-Antwort senden
                        .authenticationEntryPoint(getApiAuthenticationEntryPoint())
                );
        // .httpBasic(Customizer.withDefaults()) // HTTP Basic ist für JWT-Flow nicht mehr direkt nötig und kann entfernt werden

        return http.build();
    }

    /**
     * Sicherheitskonfiguration für die stateful Webanwendung (alle anderen Pfade).
     * Sichert die Anwendung mit einem Formular-Login. Relevant für Vaadin, wenn es serverseitig rendert.
     */
    @Bean
    @Order(2)
    public SecurityFilterChain formLoginFilterChain(HttpSecurity http) throws Exception {
        http
                .authorizeHttpRequests(authorize -> authorize
                        // Statische Ressourcen und öffentliche Seiten für alle freigeben
                        .requestMatchers("/css/**", "/js/**", "/images/**", "/favicon.ico").permitAll()
                        .requestMatchers("/error", "/app/login", "/").permitAll()
                        // Der Endpunkt für die Authentifizierung muss ebenfalls frei sein für den POST-Request des Formulars
                        .requestMatchers(HttpMethod.POST, "/authenticate").permitAll()
                        // Alle anderen Anfragen müssen authentifiziert sein
                        .anyRequest().authenticated()
                )
                .formLogin(form -> form
                        .loginPage("/app/login")           // URL zur Login-Seite in Ihrer Vaadin-App
                        .loginProcessingUrl("/authenticate") // URL, an die das Login-Formular gesendet wird (Standard für Spring Security Form-Login)
                        .defaultSuccessUrl("/app/dashboard", true) // Ziel nach erfolgreichem Login in Ihrer Vaadin-App
                        .failureUrl("/app/login?error")     // Ziel bei fehlerhaftem Login
                        .permitAll()
                )
                .logout(logout -> logout
                        .logoutUrl("/logout")
                        .logoutSuccessUrl("/app/login?logout")
                        .invalidateHttpSession(true)
                        .deleteCookies("JSESSIONID")
                        .permitAll()
                )
                // CSRF-Konfiguration für die stateful Webanwendung
                .csrf(csrf -> {
                    csrf.ignoringRequestMatchers("/h2-console/**"); // Beispiel: Wenn Sie h2-console nutzen
                });

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

    /**
     * Konfiguriert CORS (Cross-Origin Resource Sharing), um Anfragen von Ihrem Frontend zu erlauben.
     */
    @Bean
    public CorsFilter corsFilter() {
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowCredentials(true);
        // ACHTUNG: Für die Produktion sollten Sie hier spezifische URLs anstelle von "*" verwenden!
        config.setAllowedOrigins(Collections.singletonList("*"));
        config.setAllowedHeaders(Collections.singletonList("*"));
        config.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "PATCH"));
        source.registerCorsConfiguration("/**", config);
        return new CorsFilter(source);
    }
}