package com.clearhub.trade.repository;

import com.clearhub.trade.domain.Firm;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FirmRepository extends JpaRepository<Firm, Long> {

    List<Firm> findAllByOrderByIdAsc();

    Optional<Firm> findByCode(String code);
}
