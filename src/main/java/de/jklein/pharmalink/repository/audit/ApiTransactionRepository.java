package de.jklein.pharmalink.repository.audit;

import de.jklein.pharmalink.domain.audit.ApiTransaction;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApiTransactionRepository extends MongoRepository<ApiTransaction, String> {
}