package com.clearhub.trade.service;

import com.clearhub.trade.common.Money;
import com.clearhub.trade.domain.Quotation;
import com.clearhub.trade.repository.QuotationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
public class QuotationService {

    private final QuotationRepository quotationRepository;

    public Quotation getOrCreate(Long commodityId) {
        return quotationRepository.findById(commodityId).orElseGet(() -> {
            Quotation quotation = new Quotation();
            quotation.setCommodityId(commodityId);
            return quotation;
        });
    }

    public BigDecimal lastPrice(Long commodityId) {
        return quotationRepository.findById(commodityId)
                .map(Quotation::getLastPrice)
                .orElse(BigDecimal.ZERO);
    }

    @Transactional(propagation = Propagation.MANDATORY)
    public Quotation updateOnTrade(Long commodityId, BigDecimal price, BigDecimal quantity, BigDecimal amount) {
        Quotation quotation = getOrCreate(commodityId);
        BigDecimal last = Money.price(price);
        if (quotation.getOpenPrice() == null || quotation.getOpenPrice().signum() == 0) {
            quotation.setOpenPrice(last);
        }
        if (quotation.getHighPrice() == null || quotation.getHighPrice().signum() == 0
                || last.compareTo(quotation.getHighPrice()) > 0) {
            quotation.setHighPrice(last);
        }
        if (quotation.getLowPrice() == null || quotation.getLowPrice().signum() == 0
                || last.compareTo(quotation.getLowPrice()) < 0) {
            quotation.setLowPrice(last);
        }
        quotation.setLastPrice(last);
        quotation.setVolume(quotation.getVolume().add(quantity));
        quotation.setAmount(Money.money(quotation.getAmount().add(amount)));
        return quotationRepository.save(quotation);
    }

    @Transactional
    public void reset(Long commodityId, BigDecimal price) {
        Quotation quotation = getOrCreate(commodityId);
        quotation.setLastPrice(price);
        quotation.setOpenPrice(price);
        quotation.setHighPrice(price);
        quotation.setLowPrice(price);
        quotation.setVolume(BigDecimal.ZERO);
        quotation.setAmount(BigDecimal.ZERO);
        quotationRepository.save(quotation);
    }
}
