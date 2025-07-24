package de.jklein.pharmalink.repository.auth;

import de.jklein.pharmalink.domain.auth.User;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserRepository extends MongoRepository<User, String> {
    User findByUsername(String username);
}