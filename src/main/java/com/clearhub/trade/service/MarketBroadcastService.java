package com.clearhub.trade.service;

import com.clearhub.trade.dto.SettlementResult;
import com.clearhub.trade.engine.BookSnapshot;
import com.clearhub.trade.websocket.MarketWebSocketHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MarketBroadcastService {

    private static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final MarketWebSocketHandler webSocketHandler;

    public void broadcastBook(long commodityId, BookSnapshot snapshot) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("type", "book");
        message.put("commodityId", commodityId);
        message.put("bids", snapshot.bids());
        message.put("asks", snapshot.asks());
        message.put("bestBid", snapshot.bestBid());
        message.put("bestAsk", snapshot.bestAsk());
        message.put("timestamp", snapshot.timestamp());
        webSocketHandler.broadcast(commodityId, message);
    }

    public void broadcastTrade(SettlementResult trade) {
        Map<String, Object> message = new LinkedHashMap<>();
        message.put("type", "trade");
        message.put("tradeId", trade.tradeId());
        message.put("commodityId", trade.commodityId());
        message.put("price", trade.price());
        message.put("quantity", trade.quantity());
        message.put("amount", trade.amount());
        message.put("buyFirmId", trade.buyFirmId());
        message.put("sellFirmId", trade.sellFirmId());
        message.put("time", trade.createTime() == null ? "" : trade.createTime().format(TIME_FORMAT));
        webSocketHandler.broadcast(trade.commodityId(), message);
    }
}
