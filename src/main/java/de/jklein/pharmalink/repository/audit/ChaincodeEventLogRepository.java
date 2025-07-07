package de.jklein.pharmalink.repository.audit;

import de.jklein.pharmalink.domain.audit.ChaincodeEventLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface ChaincodeEventLogRepository extends JpaRepository<ChaincodeEventLog, UUID> {
}