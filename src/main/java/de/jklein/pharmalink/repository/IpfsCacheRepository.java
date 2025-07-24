package de.jklein.pharmalink.repository;

import de.jklein.pharmalink.domain.IpfsCacheEntry;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IpfsCacheRepository extends MongoRepository<IpfsCacheEntry, String> {
    Optional<IpfsCacheEntry> findByIpfsHash(String ipfsHash);
}