package com.clearhub.trade.engine;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * Price-time priority limit order book for a single commodity.
 *
 * <p>This class is the extracted core of the original matching engine: bids are
 * sorted by price descending then order id ascending, asks by price ascending then
 * order id ascending. The trade price is always the resting order's price.</p>
 *
 * <p>All mutations are synchronized; the demo runs one matching thread per commodity,
 * but snapshots are read from HTTP/WebSocket threads.</p>
 */
public class MatchEngine {

    private static final Comparator<BookOrder> BID_ORDER = (a, b) -> {
        int byPrice = b.getPrice().compareTo(a.getPrice());
        if (byPrice != 0) {
            return byPrice;
        }
        return Long.compare(a.getOrderId(), b.getOrderId());
    };

    private static final Comparator<BookOrder> ASK_ORDER = (a, b) -> {
        int byPrice = a.getPrice().compareTo(b.getPrice());
        if (byPrice != 0) {
            return byPrice;
        }
        return Long.compare(a.getOrderId(), b.getOrderId());
    };

    private final long commodityId;
    private final boolean allowSelfTrade;
    private final TreeMap<BookOrder, BookOrder> bids = new TreeMap<>(BID_ORDER);
    private final TreeMap<BookOrder, BookOrder> asks = new TreeMap<>(ASK_ORDER);
    private final Map<Long, BookOrder> index = new HashMap<>();

    public MatchEngine(long commodityId) {
        this(commodityId, false);
    }

    public MatchEngine(long commodityId, boolean allowSelfTrade) {
        this.commodityId = commodityId;
        this.allowSelfTrade = allowSelfTrade;
    }

    public synchronized List<Match> submit(BookOrder incoming) {
        List<Match> matches = new ArrayList<>();
        BigDecimal remaining = incoming.getRemaining();

        TreeMap<BookOrder, BookOrder> opposite = incoming.isBuy() ? asks : bids;
        Iterator<Map.Entry<BookOrder, BookOrder>> iterator = opposite.entrySet().iterator();
        while (remaining.signum() > 0 && iterator.hasNext()) {
            BookOrder resting = iterator.next().getValue();
            if (!allowSelfTrade && resting.getFirmId() == incoming.getFirmId()) {
                // Preserve price priority: do not jump over a better same-firm quote.
                break;
            }
            if (!crosses(incoming, resting)) {
                break;
            }
            BigDecimal quantity = remaining.min(resting.getRemaining());
            matches.add(new Match(
                    incoming.isBuy() ? incoming.getOrderId() : resting.getOrderId(),
                    incoming.isBuy() ? resting.getOrderId() : incoming.getOrderId(),
                    incoming.isBuy() ? incoming.getFirmId() : resting.getFirmId(),
                    incoming.isBuy() ? resting.getFirmId() : incoming.getFirmId(),
                    commodityId,
                    resting.getPrice(),
                    quantity,
                    incoming.isBuy() ? "B" : "S"
            ));
            remaining = remaining.subtract(quantity);
            resting.setRemaining(resting.getRemaining().subtract(quantity));
            if (resting.getRemaining().signum() == 0) {
                iterator.remove();
                index.remove(resting.getOrderId());
            }
        }

        incoming.setRemaining(remaining);
        if (remaining.signum() > 0) {
            TreeMap<BookOrder, BookOrder> own = incoming.isBuy() ? bids : asks;
            own.put(incoming, incoming);
            index.put(incoming.getOrderId(), incoming);
        }
        return matches;
    }

    private boolean crosses(BookOrder incoming, BookOrder resting) {
        return incoming.isBuy()
                ? incoming.getPrice().compareTo(resting.getPrice()) >= 0
                : incoming.getPrice().compareTo(resting.getPrice()) <= 0;
    }

    public synchronized void restore(BookOrder order) {
        if (order.getRemaining().signum() <= 0) {
            return;
        }
        TreeMap<BookOrder, BookOrder> own = order.isBuy() ? bids : asks;
        own.put(order, order);
        index.put(order.getOrderId(), order);
    }

    public synchronized boolean cancel(long orderId) {
        BookOrder order = index.remove(orderId);
        if (order == null) {
            return false;
        }
        TreeMap<BookOrder, BookOrder> own = order.isBuy() ? bids : asks;
        return own.remove(order) != null;
    }

    public synchronized BookSnapshot snapshot(int depth) {
        return new BookSnapshot(
                commodityId,
                aggregate(bids, depth),
                aggregate(asks, depth),
                bestPrice(bids),
                bestPrice(asks),
                System.currentTimeMillis()
        );
    }

    private List<BookLevel> aggregate(TreeMap<BookOrder, BookOrder> book, int depth) {
        List<BookLevel> levels = new ArrayList<>();
        BigDecimal currentPrice = null;
        BigDecimal quantity = BigDecimal.ZERO;
        int count = 0;
        for (BookOrder order : book.values()) {
            if (currentPrice == null) {
                currentPrice = order.getPrice();
            } else if (currentPrice.compareTo(order.getPrice()) != 0) {
                levels.add(new BookLevel(currentPrice, quantity, count));
                if (levels.size() >= depth) {
                    return levels;
                }
                currentPrice = order.getPrice();
                quantity = BigDecimal.ZERO;
                count = 0;
            }
            quantity = quantity.add(order.getRemaining());
            count++;
        }
        if (currentPrice != null && levels.size() < depth) {
            levels.add(new BookLevel(currentPrice, quantity, count));
        }
        return levels;
    }

    private BigDecimal bestPrice(TreeMap<BookOrder, BookOrder> book) {
        return book.isEmpty() ? null : book.firstKey().getPrice();
    }

    public synchronized int openOrderCount() {
        return index.size();
    }

    public synchronized void clear() {
        bids.clear();
        asks.clear();
        index.clear();
    }

    public long getCommodityId() {
        return commodityId;
    }
}
