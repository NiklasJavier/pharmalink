package de.jklein.pharmalink.repository.system;

import de.jklein.pharmalink.domain.system.SystemState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SystemStateRepository extends JpaRepository<SystemState, Long> {
    Optional<SystemState> findFirstByOrderByIdAsc();
}