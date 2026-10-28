# 接口文档

Base URL：`http://<host>:8088`（Nginx）或 `http://<host>:8080`（后端直连）

统一响应：

```json
{ "code": 0, "message": "ok", "data": { } }
```

- `code = 0` 表示成功；非 0 时 `data` 为 `null`，`message` 为错误原因。
- 下单/撤单需要请求头 `X-Firm-Id: <firmId>`。
- 所有金额为字符串形式的十进制数，保留 2 位；数量保留 4 位。

## 1. 市场与行情

### GET /api/firms

演示账户列表。

```json
{
  "code": 0,
  "data": [
    { "id": 1, "code": "MM001", "name": "做市商账户" },
    { "id": 2, "code": "DEMO001", "name": "演示用户" },
    { "id": 3, "code": "DEMO002", "name": "演示用户 B" }
  ]
}
```

### GET /api/commodities

商品列表，含最新价、涨跌、盘口最优价、最小变动单位、手续费率。

```json
{
  "code": 0,
  "data": [
    {
      "id": 1,
      "code": "DEMO-A",
      "name": "示例商品 A",
      "lastPrice": 100.03,
      "openPrice": 100.00,
      "highPrice": 100.13,
      "lowPrice": 99.96,
      "changePct": 0.03,
      "volume": 12,
      "amount": 1200.36,
      "minPriceChange": 0.01,
      "minAmountChange": 1,
      "feeRate": 0.0005,
      "bestBid": 99.99,
      "bestAsk": 100.03,
      "tradable": true
    }
  ]
}
```

### GET /api/market/{commodityId}/orderbook?depth=10

盘口快照。`bids` 按价格降序，`asks` 按价格升序；每档含价格、总量、委托数。

```json
{
  "code": 0,
  "data": {
    "commodityId": 1,
    "bids": [{ "price": 99.99, "quantity": 39, "orderCount": 1 }],
    "asks": [{ "price": 100.03, "quantity": 39, "orderCount": 1 }],
    "lastPrice": 100.03,
    "timestamp": 1793168182229
  }
}
```

### GET /api/market/{commodityId}/trades?limit=50

最近成交，按成交号倒序。

### GET /api/market/{commodityId}/quotation

最新行情（原始实体字段）。

## 2. 账户与查询

| 方法 | 路径 | 说明 |
|---|---|---|
| GET | `/api/funds?firmId=2` | 余额、冻结、可用 |
| GET | `/api/positions?firmId=2` | 持仓、成本价、市值、浮动盈亏 |
| GET | `/api/orders?firmId=2&status=0&limit=100` | 我的委托；`status` 可选 |
| GET | `/api/trades?firmId=2&limit=100` | 我的成交；`side` 为相对当前账户的买卖方向 |
| GET | `/api/flows?firmId=2&limit=100` | 资金流水 |

### 委托状态

| status | 含义 |
|---|---|
| 0 | 待成交 |
| 1 | 部分成交 |
| 2 | 全部成交 |
| 3 | 已撤单 |
| 4 | 废单 |

## 3. 下单与撤单

### POST /api/orders

请求头：`X-Firm-Id: 2`

```json
{
  "commodityId": 1,
  "side": "BUY",
  "price": 100.05,
  "quantity": 10
}
```

- `side`：`BUY` / `SELL`（也接受 `B` / `S`）。
- `price` 必须是最小变动价位的整数倍；`quantity` 必须是最小变动数量的整数倍。
- 买入冻结 `price × quantity × (1 + feeRate)`；卖出冻结可用持仓。
- 返回新建委托。若立即成交，撮合与结算异步完成，前端会自动刷新。

```json
{
  "code": 0,
  "data": {
    "id": 17,
    "orderNo": "O20261028141056994066",
    "firmId": 2,
    "commodityId": 1,
    "commodityCode": "DEMO-A",
    "side": "B",
    "sideLabel": "买入",
    "price": 100.05,
    "quantity": 10,
    "filledQuantity": 0,
    "remainingQuantity": 10,
    "status": 0,
    "statusLabel": "待成交",
    "frozenFunds": 1001.00,
    "fee": 0.50
  }
}
```

### POST /api/orders/{orderId}/cancel

请求头：`X-Firm-Id: 2`。撤单成功后释放剩余冻结资金/持仓。

```json
{ "code": 0, "data": { "id": 17, "status": 3, "statusLabel": "已撤单" } }
```

## 4. 演示运维

### POST /api/demo/reset

清空成交、委托、持仓、资金流水，恢复初始账户/持仓/行情，并重启撮合。仅演示使用。

### GET /api/demo/status

```json
{
  "code": 0,
  "data": {
    "status": "UP",
    "firms": 3,
    "commodities": 2,
    "orders": 24,
    "openOrders": 6,
    "trades": 12,
    "flows": 36,
    "totalVolume": 120,
    "serverTime": "2026-10-28T14:16:22"
  }
}
```

### GET /actuator/health

```json
{ "status": "UP", "components": { "db": { "status": "UP" }, "ping": { "status": "UP" } } }
```

## 5. 清算

### GET /api/clearing/summary

未清算成交数/金额、最近批次、银行/内部资金合计。

```json
{
  "code": 0,
  "data": {
    "unsettledTrades": 1,
    "unsettledAmount": 100.04,
    "lastBatchNo": "CL20261028182019143183",
    "lastBatchTime": "2026-10-28 18:20:19",
    "bankTotal": 120000000.00,
    "ledgerTotal": 119999999.80
  }
}
```

### POST /api/clearing/run

把当前未清算成交归集为一个批次，并按内部资金余额同步银行结算账户。

```json
{
  "code": 0,
  "data": {
    "id": 1,
    "batchNo": "CL20261028182019143183",
    "tradeDay": "20260101",
    "status": "COMPLETED",
    "totalTrades": 1,
    "totalAmount": 100.04,
    "totalFee": 0.10,
    "firmCount": 2,
    "details": [
      {
        "firmId": 2,
        "firmName": "演示用户",
        "commodityCode": "DEMO-A",
        "openingQty": 100000,
        "buyQty": 2,
        "sellQty": 0,
        "closingQty": 100002,
        "fee": 0.05,
        "netAmount": -100.09
      }
    ],
    "reconciliations": [
      {
        "firmId": 2,
        "firmName": "演示用户",
        "ledgerBalance": 9999899.91,
        "bankBalance": 10000000.00,
        "settledAmount": -100.09,
        "difference": -100.09,
        "status": "SETTLED"
      }
    ]
  }
}
```

> `difference` 是清算前“内部余额 − 银行余额”；`settledAmount` 是本次结算转账金额；结算后银行余额等于内部余额。`status=MATCH` 表示无需转账，`SETTLED` 表示已通过结算交易抹平。

### GET /api/clearing/batches?limit=20

批次列表（不含明细）。

### GET /api/clearing/batches/{batchId}

单个批次，含 `details` 与 `reconciliations`。

## 6. 银行

### GET /api/bank/accounts

```json
{
  "code": 0,
  "data": [
    {
      "id": 1,
      "firmId": 1,
      "firmName": "做市商账户",
      "accountNo": "BANK-MM001",
      "bankName": "演示结算银行",
      "balance": 100000200.08,
      "status": 0
    }
  ]
}
```

### GET /api/bank/transactions?firmId=2&limit=100

银行流水，`type` 为 `DEPOSIT` / `WITHDRAW` / `SETTLE`。

### POST /api/bank/deposit

```json
{ "firmId": 2, "amount": 1000, "remark": "演示入金" }
```

同时更新 `fu_account_funds` 与 `bank_account`，写资金流水和银行流水。返回银行流水号。

### POST /api/bank/withdraw

```json
{ "firmId": 2, "amount": 400, "remark": "演示出金" }
```

校验内部可用资金与银行余额；成功后同步扣减两个账户。

## 7. WebSocket

连接：`ws://<host>:8088/ws/market`

订阅：

```json
{ "type": "subscribe", "commodityId": 1 }
```

服务端消息：

```json
{ "type": "hello", "message": "connected" }
```

```json
{
  "type": "book",
  "commodityId": 1,
  "bids": [{ "price": 99.99, "quantity": 39, "orderCount": 1 }],
  "asks": [{ "price": 100.03, "quantity": 39, "orderCount": 1 }],
  "bestBid": 99.99,
  "bestAsk": 100.03,
  "timestamp": 1793168182229
}
```

```json
{
  "type": "trade",
  "tradeId": 8,
  "commodityId": 1,
  "price": 100.03,
  "quantity": 2,
  "amount": 200.06,
  "buyFirmId": 2,
  "sellFirmId": 1,
  "time": "14:17:45"
}
```

前端在断线后 3 秒重连；同时每 3 秒轮询行情作为兜底。

## 8. 错误码

| code | 含义 |
|---|---|
| 40001 | 参数校验失败 |
| 40002 | 市场未开市 |
| 40003 | 可用资金不足 |
| 40004 | 可用持仓不足 |
| 40005 | 委托不存在 |
| 40006 | 当前委托不可撤销 |
| 40007 | 价格超出涨跌停范围 |
| 40008 | 数量不符合最小变动单位 |
| 40009 | 价格不符合最小变动单位 |
| 40010 | 交易账户不存在 |
| 40011 | 商品不存在 |
| 40012 | 重复请求 |
| 50000 | 系统内部错误 |

## 9. Swagger

后端启动后访问 `http://<host>:8088/swagger-ui.html`（经 Nginx 代理）或 `http://<host>:8080/swagger-ui.html`。
