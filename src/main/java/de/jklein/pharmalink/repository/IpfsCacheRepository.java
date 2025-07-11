package de.jklein.pharmalink.repository;

import de.jklein.pharmalink.domain.IpfsCacheEntry;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IpfsCacheRepository extends JpaRepository<IpfsCacheEntry, String> {
    Optional<IpfsCacheEntry> findByIpfsHash(String ipfsHash);
}