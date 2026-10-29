# 验收清单

日期：2026-10-29  
验收对象：`trade-demo`（ClearHub 交易核心演示）

## 1. 构建与测试

| # | 检查项 | 命令 | 结果 |
|---|---|---|---|
| 1 | 后端编译打包 | `mvn -B -DskipTests package` | PASS |
| 2 | 后端自动化测试 | `mvn -B test` | PASS，14/14 |
| 3 | 前端依赖安装 | `cd web && npm ci` | PASS |
| 4 | 前端生产构建 | `cd web && npm run build` | PASS，dist 生成 |
| 5 | 后端 Docker 镜像 | `docker compose build trade-core` | PASS（Maven 依赖缓存） |
| 6 | 前端 Docker 镜像 | `docker compose build web` | PASS（使用预构建 dist） |

## 2. 运行与健康

| # | 检查项 | 命令/地址 | 结果 |
|---|---|---|---|
| 7 | MySQL 容器 | `docker compose ps` | Up (healthy) |
| 8 | 后端容器 | `docker compose ps` | Up (healthy) |
| 9 | Web 容器 | `docker compose ps` | Up，8088→80 |
| 10 | 模拟银行容器 | `docker compose ps` | Up (healthy)，8091 |
| 11 | 清算所适配器容器 | `docker compose ps` | Up (healthy)，8090 |
| 12 | 健康检查 | `GET /actuator/health` | UP，db=MySQL |
| 13 | 页面可访问 | `http://127.0.0.1:8088` | 正常渲染 |

## 3. 交易链路

| # | 检查项 | 结果 |
|---|---|---|
| 14 | 账户列表（4 个） | PASS |
| 15 | 商品列表（2 个） | PASS |
| 16 | 盘口双边 3 档 | PASS |
| 17 | 买入撮合成交 | PASS |
| 18 | 成交后持仓增加 | PASS |
| 19 | 成交后余额减少 | PASS |
| 20 | 挂单冻结资金 | PASS |
| 21 | 撤单释放冻结 | PASS |
| 22 | 卖出成交、持仓减少 | PASS |
| 23 | 涨跌停拒绝（40007） | PASS |
| 24 | 资金不足拒绝（40003） | PASS |
| 25 | 持仓不足拒绝（40004，集成测试） | PASS |
| 26 | 资金流水 4 类业务码 | PASS |
| 27 | WebSocket 盘口/成交推送 | PASS（页面无报错，轮询兜底） |
| 28 | 一键重置演示数据 | PASS |

## 4. 清算、适配器与银行链路

| # | 检查项 | 结果 |
|---|---|---|
| 29 | 未清算成交汇总 | PASS（演示中 21 笔 / 46,299.20，随模拟成交变化） |
| 30 | 执行日终清算生成批次 | PASS（1 批次 / 23 笔，明细+对账 8 行） |
| 31 | 按账户/商品生成清算明细 | PASS（期初、买入、卖出、期末、手续费、净额） |
| 32 | 银行结算账户对账 | PASS（SETTLED，结算后银行余额 = 内部余额） |
| 33 | 清算所适配器上报 | PASS（`CH-<batchNo>`，`ACCEPTED`） |
| 34 | 重新上报幂等 | PASS（ref 不变，不重复调账） |
| 35 | 模拟银行入金 | PASS（`MB...` 外部流水号，余额同步） |
| 36 | 模拟银行出金 | PASS（`MB...` 外部流水号，余额同步） |
| 37 | 超额出金拒绝（40003，集成测试） | PASS |
| 38 | 重复清算不重复归集 | PASS（第二次批次成交 0） |
| 39 | 重置同步模拟银行 | PASS（本地/模拟银行均回到 1000 万） |

## 5. 页面验收

| # | 检查项 | 结果 |
|---|---|---|
| 40 | 顶部账户/资金指标 | PASS |
| 41 | 左侧市场列表与涨跌 | PASS |
| 42 | 中间盘口/最近成交/1 分钟 K 线 + 成交量 + MA/市场深度 | PASS |
| 43 | 左侧撮合引擎状态卡、LIVE 徽标、市场脉搏 | PASS |
| 44 | 右侧下单面板（价格/数量/快捷比例）与市场信息 | PASS |
| 45 | 我的委托/成交/持仓/资金/流水 5 个页签 | PASS |
| 46 | 「交易终端 / 清算与银行」视图切换 | PASS |
| 47 | 清算页：通道状态、批次、明细、对账、执行按钮 | PASS |
| 48 | 批次回执 `ACCEPTED`、`CH-...` 与「重新上报」 | PASS |
| 49 | 银行页：账户、入金/出金弹窗、外部流水号、流水 | PASS |
| 50 | Console error / pageerror | 0 |

截图：`docs/screenshots/01-overview.png` ~ `08-bank.png`

## 6. 交付物

| # | 交付物 | 路径 |
|---|---|---|
| 51 | 源码 | `src/main/java`、`src/test/java` |
| 52 | 前端 | `web/`（含预构建 `web/dist`） |
| 53 | 建表脚本 | `db/init/01-schema.sql`（22 张表） |
| 54 | 升级脚本 | `db/migration/02-clearing.sql`、`db/migration/03-adapter.sql`、`db/migration/04-market.sql` |
| 55 | 容器编排 | `Dockerfile`、`services/*/Dockerfile`、`docker-compose.yml`、`web/nginx.conf` |
| 56 | 环境模板 | `.env.example` |
| 57 | 冒烟脚本 | `scripts/api-smoke.py`、`scripts/ui-smoke.py`、`scripts/smoke.sh` |
| 58 | 文档 | `README.md`、`docs/*.md`、`SECURITY.md`、`legacy/README.md` |

## 7. 服务器上线前需人工确认

- [ ] `.env` 中 MySQL 密码已改为强密码，且未提交到 git
- [ ] 服务器只开放 `APP_PORT`（默认 8088），不开放 3306/8080/8090/8091
- [ ] 公网演示已加 Basic Auth/VPN/HTTPS
- [ ] 公网演示设置 `DEMO_RESET_ENABLED=false`
- [ ] 不把 8090/8091 暴露到宿主机
- [ ] 如需关闭 Swagger，设置 `springdoc.api-docs.enabled=false`
- [ ] 首次启动后执行 `python3 scripts/api-smoke.py --base-url http://<host>:8088`
- [ ] 记录 `docker compose ps` 与健康检查输出

## 8. 验收结论

- 后端撮合、资金、持仓、下单/撤单/成交链路通过自动化测试与 MySQL 实例验证。
- 清算批次、明细归集、银行结算、对账、入金/出金通过自动化测试与页面验证。
- 清算所适配器、模拟银行通过 API 冒烟与页面验证：回执 `CH-...`、幂等重报、`MB...` 外部流水号、重置同步。
- 前端交易终端（实时行情、1 分钟 K 线、市场深度、撮合引擎状态）与清算/银行页面可完成完整演示，生产构建与 Nginx 代理正常。
- Docker Compose（MySQL 8 + 三个 Java 服务 + Nginx）在本机完整启动，API 冒烟 19/19，页面 0 报错，后端测试 14/14。
- 具备在 Linux + Docker 服务器上部署的条件；按 `docs/DEPLOY.md` 执行即可。
