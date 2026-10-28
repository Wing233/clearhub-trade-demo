package com.clearhub.trade.engine;

import lombok.extern.slf4j.Slf4j;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

/**
 * One matching worker per commodity. Commands (submit, cancel) are serialized through
 * a single queue so that settlement and cancel never race with the book.
 */
@Slf4j
public class MatcherWorker {

    private final MatchEngine engine;
    private final BlockingQueue<Runnable> commands = new LinkedBlockingQueue<>();
    private final Consumer<Match> matchHandler;
    private final Consumer<BookSnapshot> bookHandler;
    private volatile boolean running = true;
    private Thread worker;

    public MatcherWorker(MatchEngine engine, Consumer<Match> matchHandler, Consumer<BookSnapshot> bookHandler) {
        this.engine = engine;
        this.matchHandler = matchHandler;
        this.bookHandler = bookHandler;
    }

    public synchronized void start() {
        if (worker != null && worker.isAlive()) {
            return;
        }
        running = true;
        worker = new Thread(this::loop, "matcher-" + engine.getCommodityId());
        worker.setDaemon(true);
        worker.start();
    }

    private void loop() {
        while (running) {
            try {
                Runnable command = commands.poll(500, TimeUnit.MILLISECONDS);
                if (command != null) {
                    command.run();
                }
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
                break;
            } catch (Exception ex) {
                log.error("Matcher command failed for commodity {}", engine.getCommodityId(), ex);
            }
        }
    }

    public void submit(BookOrder order) {
        commands.add(() -> {
            List<Match> matches = engine.submit(order);
            for (Match match : matches) {
                try {
                    matchHandler.accept(match);
                } catch (Exception ex) {
                    log.error("Settlement failed for match {}", match, ex);
                }
            }
            if (bookHandler != null) {
                bookHandler.accept(engine.snapshot(10));
            }
        });
    }

    public CompletableFuture<Boolean> cancel(long orderId) {
        CompletableFuture<Boolean> result = new CompletableFuture<>();
        commands.add(() -> {
            boolean removed = engine.cancel(orderId);
            if (removed && bookHandler != null) {
                bookHandler.accept(engine.snapshot(10));
            }
            result.complete(removed);
        });
        return result;
    }

    public BookSnapshot snapshot(int depth) {
        return engine.snapshot(depth);
    }

    public void restore(BookOrder order) {
        commands.add(() -> engine.restore(order));
    }

    public synchronized void stop() {
        running = false;
        if (worker != null) {
            worker.interrupt();
            worker = null;
        }
        engine.clear();
    }

    public int openOrderCount() {
        return engine.openOrderCount();
    }

    public long commodityId() {
        return engine.getCommodityId();
    }
}
