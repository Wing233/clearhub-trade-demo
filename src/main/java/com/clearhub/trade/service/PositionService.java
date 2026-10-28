package com.clearhub.trade.service;

import com.clearhub.trade.common.BizException;
import com.clearhub.trade.common.ErrorCode;
import com.clearhub.trade.common.Money;
import com.clearhub.trade.domain.HoldDetail;
import com.clearhub.trade.domain.Position;
import com.clearhub.trade.repository.HoldDetailRepository;
import com.clearhub.trade.repository.PositionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Service
@RequiredArgsConstructor
public class PositionService {

    private final PositionRepository positionRepository;
    private final HoldDetailRepository holdDetailRepository;

    @Transactional(propagation = Propagation.MANDATORY)
    public void freeze(Long firmId, Long commodityId, BigDecimal quantity) {
        Position position = positionRepository.findForUpdate(firmId, commodityId)
                .orElseThrow(() -> new BizException(ErrorCode.INSUFFICIENT_POSITION,
                        "firmId=" + firmId + ", commodityId=" + commodityId));
        if (position.available().compareTo(quantity) < 0) {
            throw new BizException(ErrorCode.INSUFFICIENT_POSITION,
                    "需要 " + Money.qty(quantity) + "，可用 " + Money.qty(position.available()));
        }
        position.setFrozenQuantity(position.getFrozenQuantity().add(quantity));
        positionRepository.save(position);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public void release(Long firmId, Long commodityId, BigDecimal quantity) {
        if (quantity.signum() <= 0) {
            return;
        }
        Position position = positionRepository.findForUpdate(firmId, commodityId)
                .orElseThrow(() -> new BizException(ErrorCode.INSUFFICIENT_POSITION,
                        "firmId=" + firmId + ", commodityId=" + commodityId));
        position.setFrozenQuantity(Money.zeroIfNegative(position.getFrozenQuantity().subtract(quantity)));
        positionRepository.save(position);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Position applyBuy(Long firmId, Long commodityId, BigDecimal quantity, BigDecimal price,
                             BigDecimal amount, Long tradeId) {
        Position position = positionRepository.findForUpdate(firmId, commodityId)
                .orElseGet(() -> {
                    Position created = new Position();
                    created.setFirmId(firmId);
                    created.setCommodityId(commodityId);
                    return created;
                });
        position.setQuantity(position.getQuantity().add(quantity));
        position.setTotalAmount(position.getTotalAmount().add(amount));
        if (position.getQuantity().signum() > 0) {
            position.setAvgPrice(position.getTotalAmount()
                    .divide(position.getQuantity(), 4, RoundingMode.HALF_UP));
        }
        positionRepository.save(position);

        HoldDetail detail = new HoldDetail();
        detail.setHoldId(position.getId());
        detail.setTradeId(tradeId);
        detail.setFirmId(firmId);
        detail.setCommodityId(commodityId);
        detail.setInitPrice(Money.price(price));
        detail.setInitQuantity(Money.qty(quantity));
        detail.setQuantity(Money.qty(quantity));
        holdDetailRepository.save(detail);
        return position;
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Position applySell(Long firmId, Long commodityId, BigDecimal quantity) {
        Position position = positionRepository.findForUpdate(firmId, commodityId)
                .orElseThrow(() -> new BizException(ErrorCode.INSUFFICIENT_POSITION,
                        "firmId=" + firmId + ", commodityId=" + commodityId));
        if (position.getQuantity().compareTo(quantity) < 0) {
            throw new BizException(ErrorCode.INSUFFICIENT_POSITION,
                    "持仓 " + Money.qty(position.getQuantity()) + "，需要 " + Money.qty(quantity));
        }
        BigDecimal avgPrice = position.getAvgPrice();
        position.setQuantity(Money.qty(position.getQuantity().subtract(quantity)));
        position.setFrozenQuantity(Money.zeroIfNegative(position.getFrozenQuantity().subtract(quantity)));
        position.setTotalAmount(Money.money(position.getTotalAmount().subtract(avgPrice.multiply(quantity))));
        if (position.getQuantity().signum() == 0) {
            position.setAvgPrice(BigDecimal.ZERO);
            position.setTotalAmount(BigDecimal.ZERO);
        }
        positionRepository.save(position);
        return position;
    }
}
