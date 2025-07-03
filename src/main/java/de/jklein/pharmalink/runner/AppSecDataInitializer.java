package de.jklein.pharmalink.runner;

import de.jklein.pharmalink.domain.auth.Role;
import de.jklein.pharmalink.domain.auth.User;
import de.jklein.pharmalink.repository.auth.RoleRepository;
import de.jklein.pharmalink.repository.auth.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

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

        // --- 2. Testbenutzer erstellen (falls nicht vorhanden) ---

        // Admin-Benutzer
        if (userRepository.findByUsername("admin") == null) {
            User adminUser = new User();
            adminUser.setUsername("admin");
            adminUser.setPassword(passwordEncoder.encode("adminpass")); // Passwort hashen!
            adminUser.setRoles(Collections.singleton(adminRole));
            userRepository.save(adminUser);
            System.out.println("Admin-Benutzer 'admin' erstellt.");
        } else {
            System.out.println("Admin-Benutzer 'admin' existiert bereits.");
        }

        // Standard-Benutzer (z.B. für "HERSTELLER")
        if (userRepository.findByUsername("testuser") == null) {
            User testUser = new User();
            testUser.setUsername("testuser");
            testUser.setPassword(passwordEncoder.encode("testpass"));
            testUser.setRoles(Collections.singleton(userRole));
            userRepository.save(testUser);
            System.out.println("Test-Benutzer 'testuser' erstellt.");
        } else {
            System.out.println("Test-Benutzer 'testuser' existiert bereits.");
        }
    }
}