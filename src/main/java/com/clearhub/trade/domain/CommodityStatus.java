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
@Table(name = "sec_commodity_status")
public class CommodityStatus {

    @Id
    @Column(name = "commodity_id")
    private Long commodityId;

    @Column(name = "market_status", nullable = false)
    private Integer marketStatus = 2;

    @Column(name = "trade_status", nullable = false)
    private Integer tradeStatus = 1;

    public boolean isTradable() {
        return Integer.valueOf(2).equals(marketStatus) && Integer.valueOf(1).equals(tradeStatus);
    }
}
