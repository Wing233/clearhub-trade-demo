-- ClearHub trade core demo schema (MySQL 8.0)
-- The application seeds reference data on first start (demo.seed-on-start=true).

CREATE TABLE IF NOT EXISTS base_market (
  id BIGINT NOT NULL,
  code VARCHAR(32) NOT NULL,
  name VARCHAR(64) NOT NULL,
  trading_model VARCHAR(16) NOT NULL DEFAULT 'TS',
  PRIMARY KEY (id),
  UNIQUE KEY uk_market_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS sec_commodity (
  id BIGINT NOT NULL,
  market_id BIGINT NOT NULL,
  code VARCHAR(32) NOT NULL,
  name VARCHAR(64) NOT NULL,
  unit_numeral INT NOT NULL DEFAULT 1,
  min_price_change DECIMAL(18,4) NOT NULL,
  min_amount_change DECIMAL(18,4) NOT NULL,
  price_limit_pct DECIMAL(8,4) NOT NULL DEFAULT 0.1000,
  symbol_id BIGINT NOT NULL DEFAULT 1,
  status TINYINT NOT NULL DEFAULT 1,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_commodity_code (code),
  KEY idx_commodity_market (market_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS sec_commodity_status (
  commodity_id BIGINT NOT NULL,
  market_status INT NOT NULL DEFAULT 2,
  trade_status INT NOT NULL DEFAULT 1,
  PRIMARY KEY (commodity_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS sec_system_status (
  id BIGINT NOT NULL,
  status VARCHAR(2) NOT NULL,
  trade_day VARCHAR(16) NOT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS sec_trade_day (
  id BIGINT NOT NULL AUTO_INCREMENT,
  trade_day VARCHAR(16) NOT NULL,
  is_open TINYINT NOT NULL DEFAULT 1,
  PRIMARY KEY (id),
  UNIQUE KEY uk_trade_day (trade_day)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS firm (
  id BIGINT NOT NULL,
  code VARCHAR(32) NOT NULL,
  name VARCHAR(64) NOT NULL,
  status TINYINT NOT NULL DEFAULT 0,
  PRIMARY KEY (id),
  UNIQUE KEY uk_firm_code (code)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS dict_fu_symbol (
  id BIGINT NOT NULL,
  name VARCHAR(32) NOT NULL,
  PRIMARY KEY (id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS fu_account_funds (
  id BIGINT NOT NULL AUTO_INCREMENT,
  firm_id BIGINT NOT NULL,
  symbol_id BIGINT NOT NULL DEFAULT 1,
  balance DECIMAL(20,4) NOT NULL DEFAULT 0,
  frozen_funds DECIMAL(20,4) NOT NULL DEFAULT 0,
  version BIGINT NOT NULL DEFAULT 0,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_funds_firm_symbol (firm_id, symbol_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS fu_account_funds_flow (
  id BIGINT NOT NULL AUTO_INCREMENT,
  account_id BIGINT NOT NULL,
  firm_id BIGINT NOT NULL,
  biz_code VARCHAR(24) NOT NULL,
  amount DECIMAL(20,4) NOT NULL DEFAULT 0,
  frozen_amount DECIMAL(20,4) NOT NULL DEFAULT 0,
  balance_after DECIMAL(20,4) NOT NULL,
  frozen_after DECIMAL(20,4) NOT NULL,
  ref_type VARCHAR(16) DEFAULT NULL,
  ref_id BIGINT DEFAULT NULL,
  note VARCHAR(255) DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_flow_firm_time (firm_id, create_time),
  KEY idx_flow_ref (ref_type, ref_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS sec_order (
  id BIGINT NOT NULL AUTO_INCREMENT,
  order_no VARCHAR(40) NOT NULL,
  firm_id BIGINT NOT NULL,
  commodity_id BIGINT NOT NULL,
  bs_flag VARCHAR(1) NOT NULL,
  oc_flag VARCHAR(1) NOT NULL DEFAULT 'O',
  price DECIMAL(18,4) NOT NULL,
  quantity DECIMAL(18,4) NOT NULL,
  filled_quantity DECIMAL(18,4) NOT NULL DEFAULT 0,
  order_status TINYINT NOT NULL DEFAULT 0,
  frozen_funds DECIMAL(20,4) NOT NULL DEFAULT 0,
  frozen_quantity DECIMAL(18,4) NOT NULL DEFAULT 0,
  fee_rate DECIMAL(10,6) NOT NULL DEFAULT 0,
  fee DECIMAL(20,4) NOT NULL DEFAULT 0,
  settled_funds DECIMAL(20,4) NOT NULL DEFAULT 0,
  version BIGINT NOT NULL DEFAULT 0,
  trade_day VARCHAR(16) NOT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_order_no (order_no),
  KEY idx_order_firm_time (firm_id, create_time),
  KEY idx_order_book (commodity_id, order_status, bs_flag, price),
  KEY idx_order_status (order_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS sec_trade (
  id BIGINT NOT NULL AUTO_INCREMENT,
  trade_no VARCHAR(40) NOT NULL,
  commodity_id BIGINT NOT NULL,
  buy_order_id BIGINT NOT NULL,
  sell_order_id BIGINT NOT NULL,
  buy_firm_id BIGINT NOT NULL,
  sell_firm_id BIGINT NOT NULL,
  price DECIMAL(18,4) NOT NULL,
  quantity DECIMAL(18,4) NOT NULL,
  amount DECIMAL(20,4) NOT NULL,
  buy_fee DECIMAL(20,4) NOT NULL DEFAULT 0,
  sell_fee DECIMAL(20,4) NOT NULL DEFAULT 0,
  trade_day VARCHAR(16) NOT NULL,
  settled_batch_id BIGINT DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_trade_no (trade_no),
  KEY idx_trade_commodity (commodity_id, create_time),
  KEY idx_trade_buyer (buy_firm_id, create_time),
  KEY idx_trade_seller (sell_firm_id, create_time),
  KEY idx_trade_settled (settled_batch_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS sec_hold (
  id BIGINT NOT NULL AUTO_INCREMENT,
  firm_id BIGINT NOT NULL,
  commodity_id BIGINT NOT NULL,
  quantity DECIMAL(18,4) NOT NULL DEFAULT 0,
  frozen_quantity DECIMAL(18,4) NOT NULL DEFAULT 0,
  avg_price DECIMAL(18,4) NOT NULL DEFAULT 0,
  total_amount DECIMAL(20,4) NOT NULL DEFAULT 0,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_hold_firm_commodity (firm_id, commodity_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS sec_hold_detail (
  id BIGINT NOT NULL AUTO_INCREMENT,
  hold_id BIGINT NOT NULL,
  trade_id BIGINT NOT NULL,
  firm_id BIGINT NOT NULL,
  commodity_id BIGINT NOT NULL,
  init_price DECIMAL(18,4) NOT NULL,
  init_quantity DECIMAL(18,4) NOT NULL,
  quantity DECIMAL(18,4) NOT NULL,
  hold_day INT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  KEY idx_detail_hold (hold_id),
  KEY idx_detail_trade (trade_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS sec_quotation (
  commodity_id BIGINT NOT NULL,
  last_price DECIMAL(18,4) NOT NULL DEFAULT 0,
  open_price DECIMAL(18,4) NOT NULL DEFAULT 0,
  high_price DECIMAL(18,4) NOT NULL DEFAULT 0,
  low_price DECIMAL(18,4) NOT NULL DEFAULT 0,
  volume DECIMAL(20,4) NOT NULL DEFAULT 0,
  amount DECIMAL(20,4) NOT NULL DEFAULT 0,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (commodity_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS trade_param (
  commodity_id BIGINT NOT NULL,
  fee_rate DECIMAL(10,6) NOT NULL DEFAULT 0.000500,
  margin_rate DECIMAL(10,6) NOT NULL DEFAULT 0,
  max_order_qty DECIMAL(18,4) NOT NULL DEFAULT 100000,
  price_limit_pct DECIMAL(8,4) NOT NULL DEFAULT 0.1000,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (commodity_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS clear_settlement_batch (
  id BIGINT NOT NULL AUTO_INCREMENT,
  batch_no VARCHAR(40) NOT NULL,
  trade_day VARCHAR(16) NOT NULL,
  status VARCHAR(16) NOT NULL,
  total_trades INT NOT NULL DEFAULT 0,
  total_amount DECIMAL(20,4) NOT NULL DEFAULT 0,
  total_fee DECIMAL(20,4) NOT NULL DEFAULT 0,
  firm_count INT NOT NULL DEFAULT 0,
  external_ref VARCHAR(64) DEFAULT NULL,
  external_status VARCHAR(16) DEFAULT NULL,
  external_message VARCHAR(255) DEFAULT NULL,
  external_time DATETIME DEFAULT NULL,
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
  external_ref VARCHAR(64) DEFAULT NULL,
  remark VARCHAR(255) DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_bank_txn_no (txn_no),
  KEY idx_bank_txn_firm (firm_id, create_time),
  KEY idx_bank_txn_type (type)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS mock_bank_account (
  id BIGINT NOT NULL AUTO_INCREMENT,
  firm_id BIGINT NOT NULL,
  account_no VARCHAR(40) NOT NULL,
  bank_name VARCHAR(64) NOT NULL,
  currency VARCHAR(8) NOT NULL DEFAULT 'CNY',
  balance DECIMAL(20,4) NOT NULL DEFAULT 0,
  status TINYINT NOT NULL DEFAULT 0,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  update_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_mock_bank_account_no (account_no),
  UNIQUE KEY uk_mock_bank_firm (firm_id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS mock_bank_transaction (
  id BIGINT NOT NULL AUTO_INCREMENT,
  txn_no VARCHAR(40) NOT NULL,
  request_id VARCHAR(64) NOT NULL,
  firm_id BIGINT NOT NULL,
  account_no VARCHAR(40) NOT NULL,
  type VARCHAR(16) NOT NULL,
  amount DECIMAL(20,4) NOT NULL,
  balance_after DECIMAL(20,4) NOT NULL,
  ref_no VARCHAR(64) DEFAULT NULL,
  remark VARCHAR(255) DEFAULT NULL,
  create_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (id),
  UNIQUE KEY uk_mock_bank_request (request_id),
  UNIQUE KEY uk_mock_bank_txn_no (txn_no),
  KEY idx_mock_bank_account (account_no, create_time)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
