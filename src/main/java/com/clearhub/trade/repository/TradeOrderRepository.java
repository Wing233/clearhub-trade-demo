package com.clearhub.trade.repository;

import com.clearhub.trade.domain.TradeOrder;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import javax.persistence.LockModeType;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TradeOrderRepository extends JpaRepository<TradeOrder, Long> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from TradeOrder o where o.id = :id")
    Optional<TradeOrder> findForUpdate(@Param("id") Long id);

    List<TradeOrder> findByOrderStatusInOrderByIdAsc(Collection<Integer> statuses);

    List<TradeOrder> findByFirmIdOrderByIdDesc(Long firmId, Pageable pageable);

    @Query("select o from TradeOrder o where o.firmId = :firmId and (:status is null or o.orderStatus = :status) order by o.id desc")
    List<TradeOrder> findFirmOrders(@Param("firmId") Long firmId,
                                    @Param("status") Integer status,
                                    Pageable pageable);

    @Query("select count(o) from TradeOrder o where o.firmId = :firmId and o.orderStatus in (0,1)")
    long countOpenOrders(@Param("firmId") Long firmId);
}
