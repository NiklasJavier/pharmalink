package de.jklein.pharmalink.repository.auth;

import de.jklein.pharmalink.domain.auth.User;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    User findByUsername(String username);
}