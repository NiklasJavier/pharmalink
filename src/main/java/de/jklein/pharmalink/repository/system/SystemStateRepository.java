package de.jklein.pharmalink.repository.system;

import de.jklein.pharmalink.domain.system.SystemState;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SystemStateRepository extends MongoRepository<SystemState, String> {
    Optional<SystemState> findFirstByOrderByIdAsc();
    Optional<SystemState> findByCurrentActorId(String currentActorId);
}