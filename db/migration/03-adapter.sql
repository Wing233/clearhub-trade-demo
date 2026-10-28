-- Migration for databases created before the adapter/mock-bank module.
-- Fresh deployments use db/init/01-schema.sql and do NOT need this file.
-- Run once against the existing schema:
--   mysql -u root -p trade_demo < db/migration/03-adapter.sql

ALTER TABLE bank_transaction
    ADD COLUMN external_ref VARCHAR(64) DEFAULT NULL;

ALTER TABLE clear_settlement_batch
    ADD COLUMN external_ref VARCHAR(64) DEFAULT NULL,
    ADD COLUMN external_status VARCHAR(16) DEFAULT NULL,
    ADD COLUMN external_message VARCHAR(255) DEFAULT NULL,
    ADD COLUMN external_time DATETIME DEFAULT NULL;

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
