package com.clearhub.trade.repository;

import com.clearhub.trade.domain.Symbol;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SymbolRepository extends JpaRepository<Symbol, Long> {
}
