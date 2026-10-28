package com.clearhub.trade.repository;

import com.clearhub.trade.domain.FundsAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import javax.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface FundsAccountRepository extends JpaRepository<FundsAccount, Long> {

    Optional<FundsAccount> findByFirmIdAndSymbolId(Long firmId, Long symbolId);

    List<FundsAccount> findByFirmId(Long firmId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select f from FundsAccount f where f.firmId = :firmId and f.symbolId = :symbolId")
    Optional<FundsAccount> findForUpdate(@Param("firmId") Long firmId, @Param("symbolId") Long symbolId);
}
