package com.clearhub.trade.repository;

import com.clearhub.trade.domain.Position;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import javax.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface PositionRepository extends JpaRepository<Position, Long> {

    List<Position> findByFirmIdOrderByCommodityIdAsc(Long firmId);

    Optional<Position> findByFirmIdAndCommodityId(Long firmId, Long commodityId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select p from Position p where p.firmId = :firmId and p.commodityId = :commodityId")
    Optional<Position> findForUpdate(@Param("firmId") Long firmId, @Param("commodityId") Long commodityId);
}
