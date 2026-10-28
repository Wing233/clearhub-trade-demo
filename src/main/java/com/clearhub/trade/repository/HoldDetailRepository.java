package com.clearhub.trade.repository;

import com.clearhub.trade.domain.HoldDetail;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface HoldDetailRepository extends JpaRepository<HoldDetail, Long> {

    List<HoldDetail> findByFirmIdOrderByIdDesc(Long firmId);
}
