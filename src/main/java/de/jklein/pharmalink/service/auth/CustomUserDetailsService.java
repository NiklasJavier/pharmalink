package de.jklein.pharmalink.service.auth;

import de.jklein.pharmalink.domain.auth.User;
import de.jklein.pharmalink.repository.auth.UserRepository;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;

import java.util.stream.Collectors; // Benötigt für Stream-Operationen

/**
 * CustomUserDetailsService implementiert Spring Securitys UserDetailsService-Schnittstelle.
 * Sie ist verantwortlich für das Laden von Benutzerdaten aus der Datenbank,
 * die für die Authentifizierung benötigt werden.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Lade den Benutzer aus deiner Datenbank.
        // Annahme: findByUsername gibt direkt ein User-Objekt zurück oder null, wenn nicht gefunden.
        User user = userRepository.findByUsername(username);

        // Wenn der Benutzer nicht gefunden wurde, werfe eine UsernameNotFoundException.
        if (user == null) {
            throw new UsernameNotFoundException("Benutzer nicht gefunden: " + username);
        }

        // Konvertiere dein eigenes User-Objekt in ein Spring Security UserDetails-Objekt.
        // Da der Benutzer jetzt ein Set von Rollen hat (getRoles()),
        // müssen wir dieses Set in eine Collection von GrantedAuthority umwandeln.
        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(), // Das gehashte Passwort aus deiner Datenbank
                user.getRoles().stream() // Ruft das Set der Rollen ab
                        .map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName())) // Mappt jede Rolle zu einer Spring Security Authority
                        .collect(Collectors.toList()) // Sammelt die Authorities in einer Liste
        );
    }
}