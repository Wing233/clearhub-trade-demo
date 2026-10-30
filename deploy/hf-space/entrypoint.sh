#!/usr/bin/env bash
set -euo pipefail

mkdir -p /run/mysqld /var/log/nginx /app/logs
chown -R mysql:mysql /run/mysqld /var/lib/mysql

if [ ! -d /var/lib/mysql/mysql ]; then
  echo "Initializing MySQL data directory ..."
  mysqld --initialize-insecure --user=mysql --datadir=/var/lib/mysql
fi

supervisord -c /app/supervisord.conf &
SUPERVISOR_PID=$!

cleanup() {
  supervisorctl -c /app/supervisord.conf shutdown >/dev/null 2>&1 || true
  kill "${SUPERVISOR_PID}" 2>/dev/null || true
  wait "${SUPERVISOR_PID}" 2>/dev/null || true
}
trap cleanup TERM INT

echo "Waiting for MySQL ..."
for _ in $(seq 1 60); do
  if mysqladmin ping -uroot --silent >/dev/null 2>&1; then
    break
  fi
  sleep 1
done

if ! mysqladmin ping -uroot --silent >/dev/null 2>&1; then
  echo "MySQL did not start" >&2
  supervisorctl -c /app/supervisord.conf status || true
  exit 1
fi

bash /app/db-init.sh

start_and_wait() {
  local program="$1"
  local port="$2"
  supervisorctl -c /app/supervisord.conf start "${program}"
  for _ in $(seq 1 120); do
    if bash -c "exec 3<>/dev/tcp/127.0.0.1/${port}" 2>/dev/null; then
      echo "${program} is listening on ${port}"
      return 0
    fi
    sleep 1
  done
  echo "${program} did not open port ${port}" >&2
  supervisorctl -c /app/supervisord.conf status "${program}" || true
  return 1
}

start_and_wait mock-bank 8091
start_and_wait clearing-adapter 8090
start_and_wait trade-core 8080
supervisorctl -c /app/supervisord.conf start nginx

wait "${SUPERVISOR_PID}"
