package de.jklein.pharmalink.repository.auth;

import de.jklein.pharmalink.domain.auth.Role;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Role findByName(String name);
}