package de.jklein.pharmalink.repository;

import de.jklein.pharmalink.domain.Unit;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UnitRepository extends MongoRepository<Unit, String> {
    Optional<Unit> findByUnitId(String unitId);
    List<Unit> findByMedId(String medId);
    List<Unit> findByCurrentOwnerActorId(String ownerActorId);
    long countByCurrentOwnerActorId(String ownerActorId);
    void deleteByUnitId(String unitId);
    void deleteByCurrentOwnerActorId(String ownerId);
}