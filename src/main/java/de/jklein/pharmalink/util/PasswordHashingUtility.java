package de.jklein.pharmalink.util;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component; // Damit Spring diese Klasse als Bean verwalten kann

@Component
public class PasswordHashingUtility {

    private final PasswordEncoder passwordEncoder;

    public PasswordHashingUtility(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    public String hashPassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Verwendung: java -cp <your-app-jar.jar> de.jklein.pharmalink.util.PasswordHashingUtility <passwort>");
            System.out.println("Um ein Passwort zu hashen.");
            return;
        }
        String rawPassword = args[0];
        PasswordEncoder encoder = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
        String hashedPassword = encoder.encode(rawPassword);
        System.out.println("Klartext-Passwort: " + rawPassword);
        System.out.println("Gehashtes Passwort: " + hashedPassword);
    }
}