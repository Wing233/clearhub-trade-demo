---
title: ClearHub 交易核心演示
emoji: 📈
colorFrom: blue
colorTo: indigo
sdk: docker
app_port: 7860
pinned: false
license: mit
---

# ClearHub 交易核心演示

现货交易核心演示：下单 → 价格优先/时间优先撮合 → 成交 → 持仓 → 资金冻结/扣款/解冻，附带做市商、模拟交易员、K 线/市场深度、日终清算、清算所适配器与模拟银行。

## 说明

- 全部服务运行在同一个 Docker Space 内：MySQL 8 + trade-core + mock-bank + clearing-adapter + Nginx。
- 数据存放在容器内，Space 重启后会恢复初始演示数据；这是临时演示，不是生产系统。
- 无登录鉴权，任何人都可以通过 `X-Firm-Id` 操作演示账户；请勿放入真实数据。
- 首次启动需要初始化 MySQL 和导入 22 张表，通常 1–3 分钟。

## 入口

- 交易终端：`/`
- 清算与银行：页面顶部「清算与银行」
- API 健康检查：`/actuator/health`
