# 测试报告

日期：2026-10-29  
项目：ClearHub 交易核心演示（`trade-demo`）

## 1. 环境

| 项 | 版本 |
|---|---|
| OS | Windows 11 |
| 构建 JDK | Eclipse Temurin 17.0.17（Maven 运行时） |
| 手动运行 JDK | Eclipse Temurin 25.0.4.1（仅本地 H2 验证） |
| Maven | 3.9.16 |
| Node | 24.12.0 |
| 后端 | Spring Boot 2.7.18 |
| 数据库 | MySQL 8.0（Docker Compose）/ H2 2.1.214（单测与本地 h2 profile） |
| 容器 | Docker Desktop 29.9.2 / Docker Compose v2 |
| 浏览器 | Chromium（Playwright headless） |

## 2. 自动化测试（mvn test）

命令：`mvn -B test`  
结果：**14 / 14 通过，0 失败，0 错误**（`Demo data seeded: 4 firms, 2 commodities`）

### 2.1 撮合引擎单测（6 个）

`src/test/java/com/clearhub/trade/engine/MatchEngineTest.java`

| 用例 | 断言 |
|---|---|
| 买盘优先匹配最低卖价 | 卖一 99.99 先于 100.00 成交 |
| 同价时间优先 | 同价两笔卖单，先入库者先成交 |
| 部分成交 | 买 4 / 卖 10，剩余 6 继续挂单；再买 6 全部成交 |
| 禁止自成交 | 同 firm 买卖不撮合，双方留在盘口 |
| 撤单 | 撤单后盘口为空，重复撤单返回 false |
| 价格改善 | 买 101 对卖 100，成交价为 100（被动方） |

### 2.2 订单链路集成测试（5 个）

`src/test/java/com/clearhub/trade/service/OrderFlowIntegrationTest.java`  
使用 `@SpringBootTest` + H2 + 真实撮合线程 + 真实事务。

| 用例 | 断言 |
|---|---|
| 买入撮合挂单卖出 | 成交 1 笔；买方余额 −500.25，卖方余额 +499.75；买方持仓 +5，卖方持仓 −5；双方委托 FILLED；冻结归零 |
| 撤单释放冻结 | 挂买 90×10，冻结 > 0；撤单后冻结 = 0，状态 CANCELLED |
| 资金不足 | 买入 100×100000，返回 `40003` |
| 涨跌停 | 买入 200，返回 `40007` |
| 持仓不足 | 先卖 60000 冻结，再卖 60000，返回 `40004` |

### 2.3 清算与银行集成测试（3 个）

`src/test/java/com/clearhub/trade/service/ClearingIntegrationTest.java`

| 用例 | 断言 |
|---|---|
| 清算归集 | 1 笔 100×5 成交生成 1 个批次、2 条明细；买方净额 −500.25，卖方净额 +499.75；2 条对账记录均为 SETTLED；银行余额与内部余额一致；成交标记已清算 |
| 重复清算 | 同一批成交已标记，第二次清算批次成交数为 0、明细为空 |
| 出入金 | 入金 1000、出金 400 后内部资金与银行余额同步；超额出金返回 `40003` |

## 3. 页面冒烟（Playwright）

脚本：`scripts/ui-smoke.py`  
结果：**通过，0 个 console error / page error**

| 检查 | 结果 |
|---|---|
| 页面标题与品牌 | ClearHub / 交易核心演示 / LIVE |
| 市场列表 | 2 个商品 |
| 盘口 | 买一/卖一均存在，3 档买卖 |
| 账户指标 | 可用资金、冻结、余额渲染 |
| K 线与深度 | 1 分钟 K 线 + 成交量 + MA，市场深度图正常 |
| 撮合引擎卡 | 累计委托/成交、未完成委托、规则标签渲染 |
| 下单 | 按卖一价 + 1 tick 买入 2 手，生成成交 |
| 我的成交 | 1 行 |
| 持仓 | 2 行（两个商品） |
| 资金流水 | 2 行（冻结 + 成交扣款） |
| 清算页 | 未清算 21 笔 / 46,299.20，执行清算，批次 23 笔、明细/对账 8 行 |
| 银行页 | 4 个银行账户、入金 500、银行流水 4 行 |
| 截图 | `docs/screenshots/01~08` |

## 4. 手工 API 验证

在本地 H2 实例（端口 18080/8080）完成：

| 操作 | 结果 |
|---|---|
| 买入 99.77×10（Crossing） | 成交，余额 10,000,000 → 9,999,001.80，持仓 100,000 → 100,010 |
| 挂买 90×10 | 冻结 900.45，可用减少 |
| 撤单 | 冻结归零，状态已撤单 |
| 卖出 5（Crossing） | 余额增加（按买一价成交，扣手续费），持仓减少 |
| 买价 200 | `40007` 价格超出涨跌停 |
| 卖 999999 | `40001` 单笔最大数量 |
| `GET /actuator/health` | `UP`（db/ping） |

## 5. Docker / MySQL 验收（已执行）

本机 Docker Desktop 29.9.2，执行：

```bash
docker compose up -d --build
docker compose ps
```

结果：

```
clearhub-mysql             Up (healthy)   3306/tcp, 33060/tcp
clearhub-mock-bank         Up (healthy)   8091/tcp
clearhub-clearing-adapter  Up (healthy)   8090/tcp
clearhub-trade-core        Up (healthy)   8080/tcp
clearhub-web               Up             0.0.0.0:8088->80/tcp
```

健康检查：

```json
{ "status": "UP", "components": { "db": { "status": "UP", "details": { "database": "MySQL" } } } }
```

API 冒烟（经 Nginx → trade-core → MySQL）：

```
PASS  actuator health  UP
PASS  list accounts  4 accounts
PASS  list commodities  DEMO-A last=100.57
PASS  clearing adapter and mock bank are up  adapter=UP mockBank=UP
PASS  order book has two sides  bids=3 asks=4
PASS  place crossing buy  order #242 101.42
PASS  trade is produced  1 trades
PASS  position updated  quantity=100001.0
PASS  place resting buy and freeze funds  order #243 frozen=95.59
PASS  cancel releases frozen funds  cancelled, frozen=0
PASS  price limit rejected  rejected with 40007
PASS  insufficient funds rejected  rejected with 40003
PASS  clearing summary has unsettled trades  unsettled=31 amount=78512.77
PASS  run clearing batch and report to adapter  CL20261029174257744747 trades=31 ref=CH-CL20261029174257744747
PASS  retry clearing report is idempotent  idempotent ref=CH-CL20261029174257744747
PASS  bank balance reconciles with ledger  bank=9999899.53 ledger=9999899.53 ref=CH-CL20261029174257744747
PASS  bank deposit through adapter  balance 9999899.53 -> 10000899.53 ref=MB20261029174258539968
PASS  bank withdraw through adapter  balance 10000899.53 -> 10000499.53 ref=MB20261029174258770740
PASS  reset demo data and mock bank  orders=0 trades=0 bank=10000000.0

19/19 checks passed
```

> 数字随模拟交易员持续成交而变化，以上为一次实际运行的输出。

页面冒烟（生产 Nginx 静态产物 `http://127.0.0.1:8088`）：

- 盘口 3 档买 / 3 档卖；
- 1 分钟 K 线、成交量、MA5/MA10 与市场深度图正常渲染，含「含模拟历史」标记；
- 下单 2 手后成交 1 条；
- 持仓 2 条、资金流水 2 条；
- 清算页：通道状态「清算所适配器 UP / 模拟银行 UP」、未清算 21 笔、批次 1 条 / 23 笔、明细/对账 8 行、回执 `ACCEPTED`；
- 银行页：4 个账户、入金 500、银行流水 4 条、清算结算流水带 `CH-...` 外部流水号；
- `console error` / `pageerror` 均为 0；
- 截图见 `docs/screenshots/01~08`。

> 说明：前端镜像使用仓库内已构建的 `web/dist` 静态产物。若修改了前端源码，建议在服务器上用 `cd web && npm ci && npm run build` 重新构建；本机因内存紧张使用了 `web/Dockerfile.build` 在 Node 22 容器内构建并导出 `dist`。

## 6. 已知告警

| 告警 | 影响 | 处理 |
|---|---|---|
| 前端 bundle 2.16 MB（gzip 715 KB） | 首屏略慢 | 演示可接受；后续可按需引入 Element Plus / ECharts 或拆 vendor |
| H2 关闭时 `Database is already closed` | 仅测试退出日志 | 已在 H2 URL 加 `DB_CLOSE_ON_EXIT=FALSE` |
| 模拟做市商每 3 秒撤挂 | 委托/流水增长较快 | 演示可接受；长时间运行可加历史清理任务 |
| 模拟交易员每 2.5 秒成交 | 成交/资金流水持续增长 | 演示可接受；设置 `DEMO_FLOW_ENABLED=false` 可关闭 |
| 适配器/模拟银行为内存+MySQL 实现 | 非真实机构 | 演示用途；真实接入需替换为银行/清算所协议 |
| 无鉴权 | 任何人可下单/重置/出入金 | 演示环境；公网部署前按 SECURITY.md 加固 |

## 7. 结论

- 撮合规则、冻结/扣款/解冻、持仓更新、涨跌停与资金校验均通过自动化测试。
- 清算批次、按账户/商品归集、银行结算与对账、出入金通过自动化测试。
- 清算所适配器与模拟银行链路通过 API 冒烟：回执 `CH-...`、幂等重报、入金/出金 `MB...` 外部流水号、重置同步。
- 交易终端（1 分钟 K 线、市场深度、撮合引擎状态、LIVE/市场脉搏）与清算/银行页面可完成完整演示，生产构建与 Nginx 代理正常。
- Docker Compose（MySQL 8 + 三个 Java 服务 + Nginx）已在本机完整启动，API 19/19、页面 0 报错。
- 代码可构建、可测试、可容器化；服务器侧只需复制 `.env.example` 为 `.env`、修改密码、执行 `docker compose up -d --build`。
