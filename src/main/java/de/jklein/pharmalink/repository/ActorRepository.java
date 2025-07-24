package de.jklein.pharmalink.repository;

import de.jklein.pharmalink.domain.Actor;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ActorRepository extends MongoRepository<Actor, String> {
    Optional<Actor> findByActorId(String actorId);
    void deleteByActorId(String actorId);
}