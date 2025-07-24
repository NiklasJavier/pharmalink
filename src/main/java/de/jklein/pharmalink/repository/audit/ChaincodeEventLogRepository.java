package de.jklein.pharmalink.repository.audit;

import de.jklein.pharmalink.domain.audit.ChaincodeEventLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ChaincodeEventLogRepository extends MongoRepository<ChaincodeEventLog, String> {
}