package com.clearhub.trade.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "fu_account_funds_flow")
public class FundsFlow {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "account_id", nullable = false)
    private Long accountId;

    @Column(name = "firm_id", nullable = false)
    private Long firmId;

    @Column(name = "biz_code", nullable = false, length = 24)
    private String bizCode;

    @Column(nullable = false, precision = 20, scale = 4)
    private BigDecimal amount = BigDecimal.ZERO;

    @Column(name = "frozen_amount", nullable = false, precision = 20, scale = 4)
    private BigDecimal frozenAmount = BigDecimal.ZERO;

    @Column(name = "balance_after", nullable = false, precision = 20, scale = 4)
    private BigDecimal balanceAfter = BigDecimal.ZERO;

    @Column(name = "frozen_after", nullable = false, precision = 20, scale = 4)
    private BigDecimal frozenAfter = BigDecimal.ZERO;

    @Column(name = "ref_type", length = 16)
    private String refType;

    @Column(name = "ref_id")
    private Long refId;

    @Column(length = 255)
    private String note;

    @CreationTimestamp
    @Column(name = "create_time", updatable = false)
    private LocalDateTime createTime;
}
