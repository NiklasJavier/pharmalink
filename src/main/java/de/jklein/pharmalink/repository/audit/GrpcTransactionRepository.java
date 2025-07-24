package de.jklein.pharmalink.repository.audit;

import de.jklein.pharmalink.domain.audit.GrpcTransaction;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GrpcTransactionRepository extends MongoRepository<GrpcTransaction, String> {
}