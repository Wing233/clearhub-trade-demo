FROM ubuntu:22.04

ENV DEBIAN_FRONTEND=noninteractive \
    TZ=Asia/Shanghai \
    LANG=C.UTF-8

RUN printf '#!/bin/sh\nexit 101\n' > /usr/sbin/policy-rc.d \
    && chmod +x /usr/sbin/policy-rc.d \
    && apt-get update \
    && apt-get install -y --no-install-recommends \
        mysql-server mysql-client openjdk-17-jre-headless nginx supervisor curl ca-certificates tzdata \
    && rm -rf /var/lib/apt/lists/* /var/lib/mysql/* \
    && rm -f /usr/sbin/policy-rc.d

WORKDIR /app

COPY app/ /app/
COPY db/ /app/db/
COPY web/ /app/web/
COPY nginx.conf /etc/nginx/nginx.conf
COPY mysql-hf.cnf /etc/mysql/mysql.conf.d/hf.cnf
COPY supervisord.conf /app/supervisord.conf
COPY db-init.sh /app/db-init.sh
COPY entrypoint.sh /app/entrypoint.sh

RUN sed -i 's/\r$//' /app/entrypoint.sh /app/db-init.sh /app/supervisord.conf /etc/nginx/nginx.conf \
    && chmod +x /app/entrypoint.sh /app/db-init.sh \
    && mkdir -p /run/mysqld /var/log/nginx /app/logs \
    && chown -R mysql:mysql /run/mysqld /var/lib/mysql

ENV DB_HOST=127.0.0.1 \
    DB_PORT=3306 \
    DB_NAME=trade_demo \
    DB_USER=trade \
    DB_PASSWORD=clearhub_demo_pw \
    DEMO_SEED=true \
    DEMO_RESET_ENABLED=true \
    DEMO_MM_ENABLED=true \
    DEMO_FLOW_ENABLED=true \
    INTEGRATION_ENABLED=true \
    ADAPTER_BASE_URL=http://127.0.0.1:8090 \
    MOCK_BANK_BASE_URL=http://127.0.0.1:8091 \
    JAVA_OPTS="-Xms128m -Xmx512m" \
    MOCK_BANK_JAVA_OPTS="-Xms64m -Xmx192m" \
    ADAPTER_JAVA_OPTS="-Xms64m -Xmx192m"

EXPOSE 7860

ENTRYPOINT ["/app/entrypoint.sh"]
