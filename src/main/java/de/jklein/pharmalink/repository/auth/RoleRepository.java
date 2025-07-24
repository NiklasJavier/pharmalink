package de.jklein.pharmalink.repository.auth;

import de.jklein.pharmalink.domain.auth.Role;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RoleRepository extends MongoRepository<Role, String> {
    Role findByName(String name);
}