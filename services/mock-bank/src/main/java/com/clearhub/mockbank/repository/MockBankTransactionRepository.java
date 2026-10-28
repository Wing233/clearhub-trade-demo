package com.clearhub.mockbank.repository;

import com.clearhub.mockbank.domain.MockBankTransaction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface MockBankTransactionRepository extends JpaRepository<MockBankTransaction, Long> {

    Optional<MockBankTransaction> findByRequestId(String requestId);

    List<MockBankTransaction> findByAccountNoOrderByIdDesc(String accountNo, Pageable pageable);

    List<MockBankTransaction> findAllByOrderByIdDesc(Pageable pageable);
}
