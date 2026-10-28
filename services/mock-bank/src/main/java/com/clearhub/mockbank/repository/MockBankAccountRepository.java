package com.clearhub.mockbank.repository;

import com.clearhub.mockbank.domain.MockBankAccount;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import javax.persistence.LockModeType;
import java.util.List;
import java.util.Optional;

public interface MockBankAccountRepository extends JpaRepository<MockBankAccount, Long> {

    Optional<MockBankAccount> findByAccountNo(String accountNo);

    List<MockBankAccount> findAllByOrderByFirmIdAsc();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select a from MockBankAccount a where a.accountNo = :accountNo")
    Optional<MockBankAccount> findByAccountNoForUpdate(@Param("accountNo") String accountNo);
}
