package de.jklein.pharmalink.repository.audit;

import de.jklein.pharmalink.domain.audit.LoginAttempt;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LoginAttemptRepository extends MongoRepository<LoginAttempt, String> {
}