package com.clearhub.trade.domain;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "sec_system_status")
public class SystemStatus {

    @Id
    private Long id;

    @Column(nullable = false, length = 2)
    private String status;

    @Column(name = "trade_day", nullable = false, length = 16)
    private String tradeDay;

    public boolean isTrading() {
        return "T".equalsIgnoreCase(status);
    }
}
