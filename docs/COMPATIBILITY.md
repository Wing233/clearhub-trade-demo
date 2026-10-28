# 与原系统的差异说明

## 1. 背景

原始代码是一套多模块的银行/清算/交易系统，交易核心位于 `tea-sec-trade`：

- Spring Boot 2.1 / Java 8 / Spring Cloud Greenwich；
- 撮合为内存引擎，按商品分线程，价格优先、时间优先；
- 资金核心是私有 jar `jar-margin` 中的 `MarginLib`，通过 JdbcTemplate 直接读写 `fu_account_funds`；
- 依赖配置中心、ZooKeeper、Redis、RabbitMQ；
- 仓库内没有 MySQL 主库 DDL，只有 SQLite 历史查询脚本；
- **没有任何交易前端**，只有 Swagger；
- 私有 jar 与历史库 DDL 均无法取得。

因此无法把原系统“原样跑起来”。本项目采用 **“复用撮合内核 + 重写外围”** 的方式还原可演示的交易核心。

## 2. 复用的部分

| 原系统 | 本项目 | 说明 |
|---|---|---|
| 撮合规则：价格优先、时间优先、按商品分线程、成交价取被动方 | `engine/MatchEngine`、`MatcherWorker` | 规则与数据结构等价，代码为新实现 |
| `sec_order` / `sec_trade` / `sec_hold` / `sec_hold_detail` | 同名表 | 保留核心字段，新增 `frozen_funds`/`settled_funds` 等演示所需字段 |
| `sec_commodity` / `sec_quotation` / `sec_system_status` / `sec_trade_day` | 同名表 | 保留商品、行情、交易日概念 |
| `fu_account_funds` / `fu_account_funds_flow` / `firm` | 同名表 | 保留余额、冻结、流水、交易商 |
| `MarginLib` 的“余额 − 冻结 = 可用”语义 | `MarginLedger` | 只实现演示链路需要的冻结/扣款/解冻/入账 |

## 3. 重写的部分

| 模块 | 原因 |
|---|---|
| `OrderService` / `SettlementService` | 原 `MakeService` 依赖 5 个校验服务、8 张策略表、MQ 与 Redis；演示版收敛为 14 张表 |
| `MarginLedger` | `MarginLib` 为私有 jar，无法获得；按原语义重写最小实现 |
| `PositionService` | 原持仓逻辑分散在 `TradeSaveService`；演示版集中到独立服务 |
| 查询 API | 原交易服务只有 POST 写接口，无 GET 查询 |
| WebSocket | 原系统用 WebSocket 做登录/通知；演示版用于盘口/成交推送 |
| 前端 | 原系统无交易前端；本项目新写 Vue 3 交易终端 |
| 模拟做市商 | 原系统依赖外部挂牌/做市；演示版内置双边报价 |
| 建表脚本 | 原仓库无 MySQL DDL；本项目按实体与原生 SQL 反推 14 张表 |

## 4. 表结构映射

| 原表 | 本项目 | 差异 |
|---|---|---|
| `sec_order` | `sec_order` | 保留委托号、账户、商品、买卖、价格、数量、已成交、状态、冻结、交易日；新增 `fee_rate`、`settled_funds`、`version`；去掉策略、交收、仓单等字段 |
| `sec_trade` | `sec_trade` | 保留成交号、商品、买卖委托、买卖账户、价格、数量、金额、手续费、交易日；去掉交收/仓单字段 |
| `sec_hold` | `sec_hold` | 保留账户、商品、数量、冻结、成本、总额；去掉 `bs_flag`（现货只做多） |
| `sec_hold_detail` | `sec_hold_detail` | 保留持仓明细、成交、初始价/量、持有天数 |
| `sec_commodity` | `sec_commodity` | 保留代码、名称、市场、最小变动、涨跌停、币种；去掉分类/交收/仓储字段 |
| `sec_quotation` | `sec_quotation` | 保留最新/开/高/低/量/额；去掉买卖盘缓存字段（盘口从内存读） |
| `sec_system_status` / `sec_trade_day` | 同名 | 保留状态与交易日 |
| `fu_account_funds` | 同名 | 保留余额、冻结、币种、版本；去掉市场/银行相关列 |
| `fu_account_funds_flow` | 同名 | 保留业务码、金额、冻结变动、余额快照、关联单号 |
| `sec_strategy_default` + 7 张子表 | `trade_param` | 合并为手续费率、保证金率（固定 0）、单笔上限、涨跌停 |
| `clear_*` / `fb_*` / `qd_*` | 未纳入 | 银行签约、出入金、对账、清算不在本演示范围 |

## 5. 有意省略的能力

- 卖空、杠杆、保证金交易、逐日盯市；
- 交割、提货、仓单、配售、点价、协议转让；
- 银行签约/出入金/对账/清算文件/SFTP；
- UUMS/Shiro 权限、网关会话、审计日志；
- 配置中心、注册中心、Redis、RabbitMQ、MongoDB；
- 多市场、多币种、多交易节、夜盘；
- 日终结算、结算价、手续费分级、印花税。

## 6. 行为差异

| 项 | 原系统 | 本项目 |
|---|---|---|
| 资金模型 | 保证金 + 冻结 + 成交扣减，支持卖空 | 现货全额冻结，只做多 |
| 卖单入口 | HTTP 只允许买开，卖单靠预置/挂牌 | 支持买入、卖出 |
| 成交结算 | 撮合线程 + 队列 + 失败重试 | 撮合线程内同步事务，失败记日志 |
| 行情 | Redis + DB + 推送 | 内存盘口 + DB 行情 + WebSocket |
| 自成交 | `tradeSelf` 开关 | 默认禁止，同 firm 停止撮合 |
| 手续费 | 多策略表、按商品/账户 | 单一费率 `trade_param.fee_rate` |
| 鉴权 | 网关会话 + UUMS | 无，演示用 `X-Firm-Id` |

## 7. 匿名化与合规

- 项目品牌、商品、账户、数据库名、域名、端口均为中性命名；
- 未包含原公司的客户数据、真实银行卡号、身份证号、手机号、银行证书、SFTP/RSA/SM2 私钥；
- 未复制原仓库中的银行协议实现、清算文件、邮件授权码、内网 IP、SVN 地址；
- 撮合规则与表结构属于业务概念，代码为新实现；如对外发布，请再次确认你方对原代码的演绎授权。

## 8. 为什么不做“原样还原”

| 阻塞 | 影响 |
|---|---|
| `jar-common` / `jar-margin` / `jar-dao-*` 2.2.0.0 无法获取 | 交易、资金、配置、查询模块全部无法编译 |
| 无 MySQL DDL | 60+ 张表、19 个视图、字段默认值只能反推 |
| 无前端 | 只能 Swagger/Postman，无法演示订单簿/持仓/资金 |
| 配置中心/ZK/Redis/MQ | 至少 4 个中间件 + 22 个服务才能拼出完整链路 |

在“无法取得私有制品与 DDL”的前提下，原样还原的成本（约 70–100 人日）远高于抽取核心（约 15 人日），且演示效果更差。本项目选择后者。
