package com.clearhub.trade.embedded;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MockBankTransactionRepository extends JpaRepository<MockBankTransaction, Long> {

    Optional<MockBankTransaction> findByRequestId(String requestId);
}
