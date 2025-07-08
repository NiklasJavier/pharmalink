package de.jklein.pharmalink.runner;

import de.jklein.pharmalink.domain.auth.Role;
import de.jklein.pharmalink.domain.auth.User;
import de.jklein.pharmalink.repository.auth.RoleRepository;
import de.jklein.pharmalink.repository.auth.UserRepository;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.nio.file.Path;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/**
 * AppSecDataInitializer initialisiert beim Start der Anwendung
 * Sicherheitsdaten wie Benutzer und Rollen.
 * Implementiert CommandLineRunner, um beim Start der Spring Boot Anwendung ausgeführt zu werden.
 */
@Component
public class AppSecDataInitializer implements CommandLineRunner {

    @Value("${system.benutzername}")
    private String benutzername;

    @Value("${system.passwort}")
    private Path passwort;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;

    public AppSecDataInitializer(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        // --- 1. Rollen erstellen (falls nicht vorhanden) ---

        // Rolle "ADMIN"
        Role adminRole = roleRepository.findByName("ADMIN"); // findByName() gibt direkt Role oder null zurück
        if (adminRole == null) {
            adminRole = new Role();
            adminRole.setName("ADMIN");
            adminRole.setUsers(new HashSet<>()); // Leeres Set initialisieren, da Bidirektional
            adminRole = roleRepository.save(adminRole);
            System.out.println("Rolle 'ADMIN' erstellt.");
        } else {
            System.out.println("Rolle 'ADMIN' existiert bereits.");
        }


        // Rolle "USER" (oder eine andere Standardrolle, z.B. "HERSTELLER")
        Role userRole = roleRepository.findByName("USER"); // findByName() gibt direkt Role oder null zurück
        if (userRole == null) {
            userRole = new Role();
            userRole.setName("USER");
            userRole.setUsers(new HashSet<>());
            userRole = roleRepository.save(userRole);
            System.out.println("Rolle 'USER' erstellt.");
        } else {
            System.out.println("Rolle 'USER' existiert bereits.");
        }

        // Admin-Benutzer
        if (userRepository.findByUsername(benutzername.toString()) == null) {
            User adminUser = new User();
            adminUser.setUsername(benutzername.toString());
            adminUser.setPassword(passwordEncoder.encode(passwort.toString()));
            adminUser.setRoles(Collections.singleton(adminRole));
            userRepository.save(adminUser);
            System.out.println("Admin-Benutzer '" + benutzername.toString() +"' erstellt.");
        } else {
            System.out.println("Admin-Benutzer '" + benutzername.toString() + "' existiert bereits.");
        }
    }
}