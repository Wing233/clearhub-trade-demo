package com.clearhub.trade.repository;

import com.clearhub.trade.domain.BankAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import javax.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface BankAccountRepository extends JpaRepository<BankAccount, Long> {

    List<BankAccount> findAllByOrderByFirmIdAsc();

    Optional<BankAccount> findByFirmId(Long firmId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select b from BankAccount b where b.firmId = :firmId")
    Optional<BankAccount> findByFirmIdForUpdate(@Param("firmId") Long firmId);
}
