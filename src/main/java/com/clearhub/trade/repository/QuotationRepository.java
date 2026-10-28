package com.clearhub.trade.repository;

import com.clearhub.trade.domain.Quotation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface QuotationRepository extends JpaRepository<Quotation, Long> {
}
