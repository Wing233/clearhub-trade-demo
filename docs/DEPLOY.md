# 服务器部署与运维

## 1. 环境要求

| 项 | 最低 | 推荐 |
|---|---|---|
| OS | Linux x86_64 | Ubuntu 22.04 / Debian 12 |
| CPU / 内存 | 4C / 8G | 4C / 8G |
| 磁盘 | 20 GB | 30 GB |
| Docker | 24.x | 最新稳定版 |
| Docker Compose | v2.20+ | 最新稳定版 |

服务器只需安装 Docker 与 Compose，不需要 JDK、Node、Maven 或 MySQL。

## 2. 首次部署

```bash
# 1. 获取代码
git clone <your-repo-url> trade-demo
cd trade-demo

# 2. 配置环境变量
cp .env.example .env
vi .env
#   APP_PORT=8088
#   MYSQL_ROOT_PASSWORD=<强密码>
#   MYSQL_PASSWORD=<强密码>

# 3. 如需修改前端，先在宿主机重新构建（可选）
#    cd web && npm ci && npm run build && cd ..

# 4. 构建并启动
docker compose up -d --build

# 5. 等待健康
docker compose ps
# 五个服务：mysql / mock-bank / clearing-adapter / trade-core 为 healthy，web 为 running

# 6. 验证
curl -fsS http://127.0.0.1:8088/actuator/health
python3 scripts/api-smoke.py --base-url http://127.0.0.1:8088
```

打开 `http://<服务器IP>:8088`。

## 3. 端口与网络

| 端口 | 用途 | 是否对外 |
|---|---|---|
| 8088 | Nginx + 前端 + API/WS 反代 | 是 |
| 8089 | Adminer（可选 profile） | 仅内网 |
| 8090 | clearing-adapter | 否（仅 compose 网络） |
| 8091 | mock-bank | 否（仅 compose 网络） |
| 3306 | MySQL | 否（仅 compose 网络） |
| 8080 | trade-core | 否（仅 compose 网络） |

防火墙只放行 `8088`（或 80/443 反代）。**不要**把 3306、8080、8090、8091 暴露到公网。

## 4. 环境变量

| 变量 | 默认 | 说明 |
|---|---|---|
| `APP_PORT` | 8088 | 对外 HTTP 端口 |
| `MYSQL_ROOT_PASSWORD` | 必填 | MySQL root 密码 |
| `MYSQL_DATABASE` | trade_demo | 数据库名 |
| `MYSQL_USER` | trade | 应用用户 |
| `MYSQL_PASSWORD` | 必填 | 应用用户密码 |
| `DEMO_SEED` | true | 首次启动是否写入演示数据 |
| `DEMO_RESET_ENABLED` | true | 是否允许 `/api/demo/reset` |
| `DEMO_MM_ENABLED` | true | 是否启动模拟做市商 |
| `INTEGRATION_ENABLED` | true | 是否启用清算所适配器/模拟银行 |
| `ADAPTER_TIMEOUT_MS` | 5000 | 适配器 HTTP 超时 |
| `JAVA_OPTS` | -Xms256m -Xmx512m | trade-core JVM 参数 |
| `MOCK_BANK_JAVA_OPTS` | -Xms128m -Xmx256m | mock-bank JVM 参数 |
| `ADAPTER_JAVA_OPTS` | -Xms128m -Xmx256m | clearing-adapter JVM 参数 |

修改密码后，如果 `clearhub-mysql-data` 卷已存在，新密码不会生效。要么 `docker compose down -v` 重建，要么进入 MySQL 手动改密码。

## 5. 日常运维

### 升级已有环境

- v1.1 增加了清算/银行 5 张表与 `sec_trade.settled_batch_id`，对应 `db/migration/02-clearing.sql`。
- v1.2 增加了适配器/模拟银行相关字段与 2 张模拟银行表，对应 `db/migration/03-adapter.sql`。
- 全新部署：`db/init/01-schema.sql` 已包含全部 22 张表，无需额外操作。
- 从 v1.0 升级：按顺序执行 `02-clearing.sql`、`03-adapter.sql`。
  ```bash
  docker compose exec -T mysql sh -c \
    'exec mysql -u root -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE"' < db/migration/02-clearing.sql
  docker compose exec -T mysql sh -c \
    'exec mysql -u root -p"$MYSQL_ROOT_PASSWORD" "$MYSQL_DATABASE"' < db/migration/03-adapter.sql
  docker compose up -d --build
  ```
  或者演示环境直接 `docker compose down -v && docker compose up -d --build`（会清空数据）。
- 迁移脚本只应执行一次；重复执行 `ADD COLUMN` 会报列已存在。

```bash
docker compose ps
docker compose logs -f --tail=200 trade-core
docker compose logs -f --tail=200 clearing-adapter
docker compose logs -f --tail=200 mock-bank
docker compose logs -f --tail=200 mysql
docker compose restart trade-core
docker compose up -d --build          # 代码更新后重建
docker compose down                   # 停止（保留数据）
```

### 备份

```bash
mkdir -p backups
docker compose exec -T mysql sh -c \
  'exec mysqldump -u root -p"$MYSQL_ROOT_PASSWORD" --single-transaction --databases "$MYSQL_DATABASE"' \
  > backups/trade_demo_$(date +%F_%H%M).sql
```

### 恢复

```bash
docker compose stop trade-core web
cat backups/trade_demo_YYYY-MM-DD_HHMM.sql | \
  docker compose exec -T mysql sh -c 'exec mysql -u root -p"$MYSQL_ROOT_PASSWORD"'
docker compose start trade-core web
```

### 重置演示数据

```bash
docker compose down -v          # 删除 MySQL 卷，危险
docker compose up -d --build
```

或使用页面右上角「重置演示数据」（只清交易数据，不删表）。

## 6. HTTPS / 域名

推荐在宿主机再加一层 Caddy：

```caddy
demo.example.com {
    reverse_proxy 127.0.0.1:8088
}
```

Caddy 会自动申请证书。Nginx 反代需要正确转发 WebSocket 的 `Upgrade`/`Connection` 头，本项目的 `web/nginx.conf` 已处理容器内转发。

## 7. 常见问题

| 现象 | 原因 | 处理 |
|---|---|---|
| `port is already allocated` | 8088 被占用 | 改 `.env` 的 `APP_PORT` |
| `trade-core` 一直 unhealthy | MySQL 未就绪/密码错误 | `docker compose logs trade-core mysql`，核对 `.env` |
| 页面能打开但无数据 | API 502 | 看 `trade-core` 日志；确认 `depends_on` 健康 |
| WebSocket 连接失败 | 反代未转发 Upgrade | 检查 `web/nginx.conf` 的 `/ws/` 段 |
| 改了 MySQL 密码不生效 | 数据卷已存在 | `docker compose down -v` 后重建 |
| 时间显示不正确 | 容器时区 | 已设置 `TZ=Asia/Shanghai`；宿主也建议设置 |
| 页面显示清算所/模拟银行 DOWN | 适配器或模拟银行未健康 | `docker compose logs clearing-adapter mock-bank` |
| 入金/出金返回 `50001` | 适配器调用模拟银行失败 | 看 adapter 日志；确认 `mock-bank` healthy 后重试 |
| 批次回执 FAILED/PARTIAL | 模拟银行调账部分失败 | 页面「重新上报」；检查 `mock-bank` 日志与余额 |
| `mock-bank` 启动报表不存在 | 旧数据卷缺少模拟银行表 | 执行 `db/migration/03-adapter.sql`，或 `down -v` 重建 |
| 容器 OOM | JVM 堆过大 | 调小 `JAVA_OPTS` / `MOCK_BANK_JAVA_OPTS` / `ADAPTER_JAVA_OPTS` |
| 重置接口 404/失败 | `DEMO_RESET_ENABLED=false` | 改回 `true` 并重启 |

## 8. 生产化前的加固清单

- [ ] 关闭演示重置：`DEMO_RESET_ENABLED=false`
- [ ] 关闭 Swagger：`springdoc.api-docs.enabled=false`、`springdoc.swagger-ui.enabled=false`
- [ ] 限制 Actuator：只暴露 `health`
- [ ] 不要把 8090（适配器）、8091（模拟银行）映射到宿主机
- [ ] 加登录鉴权与 `X-Firm-Id` 绑定，不能信任客户端请求头
- [ ] 加 HTTPS、WAF/限流、审计日志
- [ ] MySQL 使用独立强密码、定期备份、只读账号分离
- [ ] 接入真实行情/风控/清算，替换模拟做市商与模拟银行
- [ ] 对结算链路做幂等、补偿、对账
- [ ] 做压测与故障注入

详见 [SECURITY.md](../SECURITY.md)。
