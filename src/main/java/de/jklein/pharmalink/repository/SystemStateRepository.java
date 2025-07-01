package de.jklein.pharmalink.repository;

import de.jklein.pharmalink.domain.SystemState;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface SystemStateRepository extends JpaRepository<SystemState, Long> {

    /**
     * Findet den ersten (und einzigen) SystemState-Eintrag.
     * Da wir immer nur einen Zustand speichern, ist dies ein zuverlässiger Weg, ihn zu bekommen.
     * @return Ein Optional, das den SystemState enthält, falls vorhanden.
     */
    Optional<SystemState> findFirstByOrderByIdAsc();
}