package de.jklein.pharmalink.repository;

import de.jklein.pharmalink.domain.Medikament;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface MedikamentRepository extends MongoRepository<Medikament, String> {
    Optional<Medikament> findByMedId(String medId);
    List<Medikament> findByHerstellerId(String herstellerId);
    void deleteByMedId(String medId);
}