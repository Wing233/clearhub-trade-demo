package com.clearhub.trade.repository;

import com.clearhub.trade.domain.TradeRecord;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.math.BigDecimal;
import java.util.List;

public interface TradeRecordRepository extends JpaRepository<TradeRecord, Long> {

    List<TradeRecord> findByCommodityIdOrderByIdDesc(Long commodityId, Pageable pageable);

    @Query("select t from TradeRecord t where t.buyFirmId = :firmId or t.sellFirmId = :firmId order by t.id desc")
    List<TradeRecord> findFirmTrades(@Param("firmId") Long firmId, Pageable pageable);

    @Query("select coalesce(sum(t.quantity), 0) from TradeRecord t")
    BigDecimal sumQuantity();
}
