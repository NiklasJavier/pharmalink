package de.jklein.pharmalink.util;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component; // Damit Spring diese Klasse als Bean verwalten kann

/**
 * Eine Utility-Klasse zum Hashen von Passwörtern unter Verwendung des
 * von Spring Security konfigurierten PasswordEncoders.
 * Diese Klasse kann in anderen Komponenten injiziert werden, um Passwörter sicher zu hashen.
 */
@Component // Markiert diese Klasse als Spring Component, damit sie als Bean verwaltet werden kann
public class PasswordHashingUtility {

    private final PasswordEncoder passwordEncoder;

    /**
     * Konstruktor, durch den Spring den konfigurierten PasswordEncoder injiziert.
     * @param passwordEncoder Der Spring Security PasswordEncoder (z.B. BCryptPasswordEncoder).
     */
    public PasswordHashingUtility(PasswordEncoder passwordEncoder) {
        this.passwordEncoder = passwordEncoder;
    }

    /**
     * Hashes ein gegebenes Klartext-Passwort.
     * @param rawPassword Das Klartext-Passwort, das gehasht werden soll.
     * @return Das gehashte Passwort.
     */
    public String hashPassword(String rawPassword) {
        return passwordEncoder.encode(rawPassword);
    }

    // Optional: Eine main-Methode, um Passwörter direkt über die Kommandozeile zu hashen
    // Nützlich, wenn Sie Passwörter manuell für SQL-Inserts oder Konfigurationsdateien generieren möchten.
    // Beachten Sie, dass diese main-Methode nicht Teil des Spring-Kontextes ist, wenn sie direkt ausgeführt wird.
    // Daher wird hier direkt ein BCryptPasswordEncoder instanziiert.
    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Verwendung: java -cp <your-app-jar.jar> de.jklein.pharmalink.util.PasswordHashingUtility <passwort>");
            System.out.println("Um ein Passwort zu hashen.");
            return;
        }
        String rawPassword = args[0];
        // Für die direkte Nutzung in main() ohne Spring-Kontext:
        PasswordEncoder encoder = new org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder();
        String hashedPassword = encoder.encode(rawPassword);
        System.out.println("Klartext-Passwort: " + rawPassword);
        System.out.println("Gehashtes Passwort: " + hashedPassword);
    }
}