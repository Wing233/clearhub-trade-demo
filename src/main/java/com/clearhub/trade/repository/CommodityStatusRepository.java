package com.clearhub.trade.repository;

import com.clearhub.trade.domain.CommodityStatus;
import org.springframework.data.jpa.repository.JpaRepository;

public interface CommodityStatusRepository extends JpaRepository<CommodityStatus, Long> {
}
