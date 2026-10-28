package com.clearhub.trade.repository;

import com.clearhub.trade.domain.SettlementBatch;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface SettlementBatchRepository extends JpaRepository<SettlementBatch, Long> {

    List<SettlementBatch> findAllByOrderByIdDesc(Pageable pageable);

    Optional<SettlementBatch> findTopByOrderByIdDesc();
}
