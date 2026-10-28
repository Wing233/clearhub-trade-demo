package com.clearhub.trade.repository;

import com.clearhub.trade.domain.BankTransaction;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface BankTransactionRepository extends JpaRepository<BankTransaction, Long> {

    List<BankTransaction> findByFirmIdOrderByIdDesc(Long firmId, Pageable pageable);

    List<BankTransaction> findAllByOrderByIdDesc(Pageable pageable);
}
