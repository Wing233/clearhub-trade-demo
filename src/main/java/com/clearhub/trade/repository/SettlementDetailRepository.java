package com.clearhub.trade.repository;

import com.clearhub.trade.domain.SettlementDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SettlementDetailRepository extends JpaRepository<SettlementDetail, Long> {

    List<SettlementDetail> findByBatchIdOrderByFirmIdAscCommodityIdAsc(Long batchId);
}
