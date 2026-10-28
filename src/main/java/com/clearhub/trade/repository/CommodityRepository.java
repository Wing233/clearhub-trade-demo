package com.clearhub.trade.repository;

import com.clearhub.trade.domain.Commodity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CommodityRepository extends JpaRepository<Commodity, Long> {

    List<Commodity> findByStatusOrderByIdAsc(Integer status);

    Optional<Commodity> findByCode(String code);
}
