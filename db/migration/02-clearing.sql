-- Migration for databases created before the clearing/bank module.
-- Fresh deployments use db/init/01-schema.sql and do NOT need this file.
-- Run once against the existing schema:
--   mysql -u root -p trade_demo < db/migration/02-clearing.sql

ALTER TABLE sec_trade
    ADD COLUMN settled_batch_id BIGINT DEFAULT NULL,
    ADD KEY idx_trade_settled (settled_batch_id);

CREATE TABLE IF NOT EXISTS clear_settlement_batch (
  id BIGINT NOT NULL AUTO_INCREMENT,
  batch_no VARCHAR(40) NOT NULL,
  trade_day VARCHAR(16) NOT NULL,
  status VARCHAR(16) NOT NULL,
  total_trades INT NOT NULL DEFAULT 0,
  total_amount DECIMAL(20,4) NOT NULL DEFAULT 0,
  total_fee DECIMAL(20,4) NOT NULL DEFAULT 0,
  firm_count INT NOT NULL DEFAULT 0,
  start_time DATETIME DEFAULT NULL,
  end_time DATETIME DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_clear_batch_no (batch_no),
  KEY idx_clear_batch_day (trade_day)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS clear_settlement_detail (
  id BIGINT NOT NULL AUTO_INCREMENT,
  batch_id BIGINT NOT NULL,
  firm_id BIGINT NOT NULL,
  commodity_id BIGINT NOT NULL,
  opening_qty DECIMAL(18,4) NOT NULL DEFAULT 0,
  closing_qty DECIMAL(18,4) NOT NULL DEFAULT 0,
  buy_qty DECIMAL(18,4) NOT NULL DEFAULT 0,
  sell_qty DECIMAL(18,4) NOT NULL DEFAULT 0,
  buy_amount DECIMAL(20,4) NOT NULL DEFAULT 0,
  sell_amount DECIMAL(20,4) NOT NULL DEFAULT 0,
  fee DECIMAL(20,4) NOT NULL DEFAULT 0,
  net_amount DECIMAL(20,4) NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_clear_detail_batch (batch_id),
  KEY idx_clear_detail_firm (firm_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS clear_reconciliation (
  id BIGINT NOT NULL AUTO_INCREMENT,
  batch_id BIGINT NOT NULL,
  firm_id BIGINT NOT NULL,
  ledger_balance DECIMAL(20,4) NOT NULL DEFAULT 0,
  bank_balance DECIMAL(20,4) NOT NULL DEFAULT 0,
  settled_amount DECIMAL(20,4) NOT NULL DEFAULT 0,
  difference DECIMAL(20,4) NOT NULL DEFAULT 0,
  status VARCHAR(16) NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_clear_recon_batch (batch_id),
  KEY idx_clear_recon_firm (firm_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS bank_account (
  id BIGINT NOT NULL AUTO_INCREMENT,
  firm_id BIGINT NOT NULL,
  account_no VARCHAR(40) NOT NULL,
  bank_name VARCHAR(64) NOT NULL,
  balance DECIMAL(20,4) NOT NULL DEFAULT 0,
  status TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_bank_account_firm (firm_id),
  UNIQUE KEY uk_bank_account_no (account_no)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS bank_transaction (
  id BIGINT NOT NULL AUTO_INCREMENT,
  txn_no VARCHAR(40) NOT NULL,
  firm_id BIGINT NOT NULL,
  account_id BIGINT NOT NULL,
  type VARCHAR(16) NOT NULL,
  amount DECIMAL(20,4) NOT NULL,
  balance_after DECIMAL(20,4) NOT NULL,
  remark VARCHAR(255) DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_bank_txn_no (txn_no),
  KEY idx_bank_txn_firm (firm_id, create_time),
  KEY idx_bank_txn_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
