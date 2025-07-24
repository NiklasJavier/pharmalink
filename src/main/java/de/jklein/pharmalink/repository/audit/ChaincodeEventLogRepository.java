package de.jklein.pharmalink.repository.audit;

import de.jklein.pharmalink.domain.audit.ChaincodeEventLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ChaincodeEventLogRepository extends MongoRepository<ChaincodeEventLog, String> {
}