# 验收清单

日期：2026-10-28  
验收对象：`trade-demo`（ClearHub 交易核心演示）

## 1. 构建与测试

| # | 检查项 | 命令 | 结果 |
|---|---|---|---|
| 1 | 后端编译打包 | `mvn -B -DskipTests package` | PASS |
| 2 | 后端自动化测试 | `mvn -B test` | PASS，11/11 |
| 3 | 前端依赖安装 | `cd web && npm ci` | PASS |
| 4 | 前端生产构建 | `cd web && npm run build` | PASS，dist 生成 |
| 5 | 后端 Docker 镜像 | `docker compose build trade-core` | PASS |
| 6 | 前端 Docker 镜像 | `docker compose build web` | PASS（使用预构建 dist） |

## 2. 运行与健康

| # | 检查项 | 命令/地址 | 结果 |
|---|---|---|---|
| 7 | MySQL 容器 | `docker compose ps` | Up (healthy) |
| 8 | 后端容器 | `docker compose ps` | Up (healthy) |
| 9 | Web 容器 | `docker compose ps` | Up，8088→80 |
| 10 | 健康检查 | `GET /actuator/health` | UP，db=MySQL |
| 11 | 页面可访问 | `http://127.0.0.1:8088` | 正常渲染 |

## 3. 业务链路

| # | 检查项 | 结果 |
|---|---|---|
| 12 | 账户列表（3 个） | PASS |
| 13 | 商品列表（2 个） | PASS |
| 14 | 盘口双边 3 档 | PASS |
| 15 | 买入撮合成交 | PASS |
| 16 | 成交后持仓增加 | PASS |
| 17 | 成交后余额减少 | PASS |
| 18 | 挂单冻结资金 | PASS |
| 19 | 撤单释放冻结 | PASS |
| 20 | 卖出成交、持仓减少 | PASS |
| 21 | 涨跌停拒绝（40007） | PASS |
| 22 | 资金不足拒绝（40003） | PASS |
| 23 | 持仓不足拒绝（40004，集成测试） | PASS |
| 24 | 资金流水 4 类业务码 | PASS |
| 25 | WebSocket 盘口/成交推送 | PASS（页面无报错，轮询兜底） |
| 26 | 一键重置演示数据 | PASS |

## 4. 页面验收

| # | 检查项 | 结果 |
|---|---|---|
| 27 | 顶部账户/资金指标 | PASS |
| 28 | 左侧市场列表与涨跌 | PASS |
| 29 | 中间盘口/最近成交/价格走势 | PASS |
| 30 | 右侧下单面板（价格/数量/快捷比例） | PASS |
| 31 | 我的委托/成交/持仓/资金/流水 5 个页签 | PASS |
| 32 | Console error / pageerror | 0 |

截图：`docs/screenshots/01-overview.png` ~ `05-flows.png`

## 5. 交付物

| # | 交付物 | 路径 |
|---|---|---|
| 33 | 源码 | `src/main/java`、`src/test/java` |
| 34 | 前端 | `web/`（含预构建 `web/dist`） |
| 35 | 建表脚本 | `db/init/01-schema.sql` |
| 36 | 容器编排 | `Dockerfile`、`web/Dockerfile`、`docker-compose.yml`、`web/nginx.conf` |
| 37 | 环境模板 | `.env.example` |
| 38 | 冒烟脚本 | `scripts/api-smoke.py`、`scripts/ui-smoke.py`、`scripts/smoke.sh` |
| 39 | 文档 | `README.md`、`docs/*.md`、`SECURITY.md`、`legacy/README.md` |

## 6. 服务器上线前需人工确认

- [ ] `.env` 中 MySQL 密码已改为强密码，且未提交到 git
- [ ] 服务器只开放 `APP_PORT`（默认 8088），不开放 3306/8080
- [ ] 公网演示已加 Basic Auth/VPN/HTTPS
- [ ] 公网演示设置 `DEMO_RESET_ENABLED=false`
- [ ] 如需关闭 Swagger，设置 `springdoc.api-docs.enabled=false`
- [ ] 首次启动后执行 `python3 scripts/api-smoke.py --base-url http://<host>:8088`
- [ ] 记录 `docker compose ps` 与健康检查输出

## 7. 验收结论

- 后端撮合、资金、持仓、下单/撤单/成交链路通过自动化测试与 MySQL 实例验证。
- 前端交易终端可完成完整演示，生产构建与 Nginx 代理正常。
- Docker Compose 在本机完整启动，API 冒烟 12/12，页面 0 报错。
- 具备在 Linux + Docker 服务器上部署的条件；按 `docs/DEPLOY.md` 执行即可。
