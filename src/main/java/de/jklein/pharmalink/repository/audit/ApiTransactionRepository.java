package de.jklein.pharmalink.repository.audit;

import de.jklein.pharmalink.domain.audit.ApiTransaction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ApiTransactionRepository extends JpaRepository<ApiTransaction, Long> {
}