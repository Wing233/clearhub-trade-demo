package com.clearhub.trade.repository;

import com.clearhub.trade.domain.Market;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MarketRepository extends JpaRepository<Market, Long> {
}
