package com.clearhub.trade.repository;

import com.clearhub.trade.domain.FundsFlow;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FundsFlowRepository extends JpaRepository<FundsFlow, Long> {

    List<FundsFlow> findByFirmIdOrderByIdDesc(Long firmId, Pageable pageable);
}
