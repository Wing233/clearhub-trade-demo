package com.clearhub.trade.engine;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class MatchEngineTest {

    private BookOrder order(long id, long firmId, String side, String price, String quantity) {
        return new BookOrder(id, firmId, side, new BigDecimal(price), new BigDecimal(quantity),
                LocalDateTime.now());
    }

    @Test
    void buyMatchesBestAskFirst() {
        MatchEngine engine = new MatchEngine(1L);
        engine.submit(order(1, 11, "S", "100.00", "10"));
        engine.submit(order(2, 12, "S", "99.99", "10"));

        List<Match> matches = engine.submit(order(3, 21, "B", "100.00", "5"));

        assertThat(matches).hasSize(1);
        assertThat(matches.get(0).sellOrderId()).isEqualTo(2L);
        assertThat(matches.get(0).price()).isEqualByComparingTo("99.99");
        assertThat(matches.get(0).quantity()).isEqualByComparingTo("5");
    }

    @Test
    void samePriceUsesTimePriority() {
        MatchEngine engine = new MatchEngine(1L);
        engine.submit(order(1, 11, "S", "100.00", "10"));
        engine.submit(order(2, 12, "S", "100.00", "10"));

        List<Match> matches = engine.submit(order(3, 21, "B", "100.00", "10"));

        assertThat(matches).hasSize(1);
        assertThat(matches.get(0).sellOrderId()).isEqualTo(1L);
    }

    @Test
    void partialFillLeavesRemainingOnBook() {
        MatchEngine engine = new MatchEngine(1L);
        engine.submit(order(1, 11, "S", "100.00", "10"));

        List<Match> first = engine.submit(order(2, 21, "B", "100.00", "4"));
        assertThat(first).hasSize(1);
        assertThat(engine.snapshot(10).asks()).hasSize(1);
        assertThat(engine.snapshot(10).asks().get(0).quantity()).isEqualByComparingTo("6");

        List<Match> second = engine.submit(order(3, 22, "B", "100.00", "6"));
        assertThat(second).hasSize(1);
        assertThat(second.get(0).quantity()).isEqualByComparingTo("6");
        assertThat(engine.snapshot(10).asks()).isEmpty();
    }

    @Test
    void selfTradeIsNotAllowed() {
        MatchEngine engine = new MatchEngine(1L);
        engine.submit(order(1, 11, "S", "100.00", "10"));

        List<Match> matches = engine.submit(order(2, 11, "B", "100.00", "10"));

        assertThat(matches).isEmpty();
        BookSnapshot snapshot = engine.snapshot(10);
        assertThat(snapshot.asks()).hasSize(1);
        assertThat(snapshot.bids()).hasSize(1);
    }

    @Test
    void cancelRemovesOrder() {
        MatchEngine engine = new MatchEngine(1L);
        engine.submit(order(1, 11, "S", "100.00", "10"));

        assertThat(engine.cancel(1L)).isTrue();
        assertThat(engine.snapshot(10).asks()).isEmpty();
        assertThat(engine.cancel(1L)).isFalse();
    }

    @Test
    void priceImprovementUsesRestingPrice() {
        MatchEngine engine = new MatchEngine(1L);
        engine.submit(order(1, 11, "S", "100.00", "10"));

        List<Match> matches = engine.submit(order(2, 21, "B", "101.00", "10"));

        assertThat(matches).hasSize(1);
        assertThat(matches.get(0).price()).isEqualByComparingTo("100.00");
    }
}
