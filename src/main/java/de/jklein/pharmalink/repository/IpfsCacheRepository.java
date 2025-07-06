package de.jklein.pharmalink.repository;

import de.jklein.pharmalink.domain.IpfsCacheEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

/**
 * Repository für den Zugriff auf den IPFS-Cache in der Datenbank.
 */
@Repository
public interface IpfsCacheRepository extends JpaRepository<IpfsCacheEntry, String> {
    // JpaRepository bietet bereits findById, save, etc.
    Optional<IpfsCacheEntry> findByIpfsHash(String ipfsHash);
}