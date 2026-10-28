package com.clearhub.trade.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.UpdateTimestamp;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.GenerationType;
import javax.persistence.Id;
import javax.persistence.Table;
import javax.persistence.Version;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "fu_account_funds")
public class FundsAccount {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "firm_id", nullable = false)
    private Long firmId;

    @Column(name = "symbol_id", nullable = false)
    private Long symbolId = 1L;

    @Column(nullable = false, precision = 20, scale = 4)
    private BigDecimal balance = BigDecimal.ZERO;

    @Column(name = "frozen_funds", nullable = false, precision = 20, scale = 4)
    private BigDecimal frozenFunds = BigDecimal.ZERO;

    @Version
    @Column(nullable = false)
    private Long version;

    @UpdateTimestamp
    @Column(name = "update_time")
    private LocalDateTime updateTime;

    public BigDecimal available() {
        return balance.subtract(frozenFunds);
    }
}
