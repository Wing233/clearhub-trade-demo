package com.clearhub.trade.repository;

import com.clearhub.trade.domain.TradeDay;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TradeDayRepository extends JpaRepository<TradeDay, Long> {
}
