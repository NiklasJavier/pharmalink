package de.jklein.pharmalink.domain.auth;

import jakarta.persistence.*;
import java.util.Set; // Benötigt für die Beziehung
import lombok.Getter;
import lombok.Setter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.ToString;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "roles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@ToString(exclude = {"users"}) // Schließt die 'users'-Sammlung aus, um zyklische Abhängigkeiten in toString zu vermeiden
@EqualsAndHashCode(exclude = {"users"}) // Schließt die 'users'-Sammlung aus
public class Role {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String name; // z.B. "ROLE_USER", "ROLE_ADMIN"

    @ManyToMany(mappedBy = "roles")
    private Set<User> users;

    // Kein manueller Konstruktor oder Getter/Setter mehr nötig
}