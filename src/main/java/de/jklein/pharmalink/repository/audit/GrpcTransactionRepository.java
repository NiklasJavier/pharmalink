package de.jklein.pharmalink.repository.audit;

import de.jklein.pharmalink.domain.audit.GrpcTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface GrpcTransactionRepository extends JpaRepository<GrpcTransaction, Long> {
}