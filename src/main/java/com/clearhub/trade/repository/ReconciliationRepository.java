package com.clearhub.trade.repository;

import com.clearhub.trade.domain.Reconciliation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ReconciliationRepository extends JpaRepository<Reconciliation, Long> {

    List<Reconciliation> findByBatchIdOrderByFirmIdAsc(Long batchId);
}
