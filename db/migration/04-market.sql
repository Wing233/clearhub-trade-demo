-- Migration for databases created before the market presentation update.
-- Adds the taker side used by the tape/chart.
-- Run once against the existing schema:
--   mysql -u root -p trade_demo < db/migration/04-market.sql

ALTER TABLE sec_trade
    ADD COLUMN taker_side VARCHAR(1) DEFAULT NULL;
