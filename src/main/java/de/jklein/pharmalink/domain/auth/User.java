package de.jklein.pharmalink.domain.auth;

import jakarta.persistence.*;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.ToString;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "users")
@Getter // Generiert alle Getter-Methoden
@Setter // Generiert alle Setter-Methoden
@NoArgsConstructor // Generiert einen Konstruktor ohne Argumente
@AllArgsConstructor // Generiert einen Konstruktor mit allen Feldern als Argumenten
@ToString(exclude = {"password"}) // Generiert eine toString-Methode, schließt 'password' aus Sicherheitsgründen aus
@EqualsAndHashCode(exclude = {"password", "roles"}) // Generiert equals() und hashCode(), schließt sensitive/zyklische Felder aus
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password; // Passwort muss gehasht gespeichert werden

    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(
            name = "user_roles",
            joinColumns = @JoinColumn(name = "user_id"),
            inverseJoinColumns = @JoinColumn(name = "role_id")
    )
    private Set<Role> roles;

    // Manuell hinzugefügter Konstruktor, falls @AllArgsConstructor nicht 100% passt
    // Oder wenn du einen spezifischen Konstruktor für die Geschäftslogik benötigst
    // public User(String username, String password, Set<Role> roles) {
    //     this.username = username;
    //     this.password = password;
    //     this.roles = roles;
    // }
}