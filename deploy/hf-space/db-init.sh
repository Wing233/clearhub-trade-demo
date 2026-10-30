#!/usr/bin/env bash
set -euo pipefail

DB_NAME="${DB_NAME:-trade_demo}"
DB_USER="${DB_USER:-trade}"
DB_PASSWORD="${DB_PASSWORD:-clearhub_demo_pw}"

mysql -uroot <<SQL
CREATE DATABASE IF NOT EXISTS \`${DB_NAME}\` CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci;
CREATE USER IF NOT EXISTS '${DB_USER}'@'%' IDENTIFIED BY '${DB_PASSWORD}';
GRANT ALL PRIVILEGES ON \`${DB_NAME}\`.* TO '${DB_USER}'@'%';
FLUSH PRIVILEGES;
SQL

COUNT=$(mysql -uroot -N -B -e "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema='${DB_NAME}'")
if [ "${COUNT}" = "0" ]; then
  echo "Importing ClearHub schema into ${DB_NAME} ..."
  mysql -uroot "${DB_NAME}" < /app/db/01-schema.sql
fi
