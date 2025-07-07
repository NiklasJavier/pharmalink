package de.jklein.pharmalink.repository.system;

import de.jklein.pharmalink.domain.system.SystemState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SystemStateRepository extends JpaRepository<SystemState, String> { // <<-- MUSS HIER STRING SEIN
    Optional<SystemState> findFirstByOrderByIdAsc();
    Optional<SystemState> findByCurrentActorId(String currentActorId);
}