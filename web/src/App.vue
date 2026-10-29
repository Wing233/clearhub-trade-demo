<script setup>
import { computed, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from './api'
import { changeClass, fmt, fmtQty, num } from './utils'
import MarketList from './components/MarketList.vue'
import PriceChart from './components/PriceChart.vue'
import OrderBook from './components/OrderBook.vue'
import TradeTape from './components/TradeTape.vue'
import OrderForm from './components/OrderForm.vue'
import OrdersTable from './components/OrdersTable.vue'
import TradesTable from './components/TradesTable.vue'
import PositionsTable from './components/PositionsTable.vue'
import FundsPanel from './components/FundsPanel.vue'
import FlowsTable from './components/FlowsTable.vue'
import ClearingView from './components/ClearingView.vue'
import MarketInfo from './components/MarketInfo.vue'

const firms = ref([])
const firmId = ref(Number(localStorage.getItem('clearhub.firmId') || 2))
const view = ref('trade')
const commodities = ref([])
const currentId = ref(null)
const book = ref({ bids: [], asks: [] })
const marketTrades = ref([])
const candles = ref([])
const engine = ref(null)
const orders = ref([])
const myTrades = ref([])
const positions = ref([])
const funds = ref({ balance: 0, frozenFunds: 0, available: 0 })
const flows = ref([])
const tab = ref('orders')
const resetting = ref(false)
const wsConnected = ref(false)
const orderFormRef = ref(null)
const now = ref(new Date())

let socket = null
let reconnectTimer = null
let marketTimer = null
let accountTimer = null
let accountRefreshTimer = null
let clockTimer = null
let destroyed = false

const currentCommodity = computed(
  () => commodities.value.find((item) => item.id === currentId.value) || null,
)
const currentPosition = computed(
  () => positions.value.find((item) => item.commodityId === currentId.value) || null,
)
const firmName = computed(
  () => firms.value.find((item) => item.id === firmId.value)?.name || '',
)
const lastPrice = computed(
  () => currentCommodity.value?.lastPrice ?? book.value?.lastPrice ?? 0,
)
const changePct = computed(() => currentCommodity.value?.changePct ?? 0)
const bestBid = computed(
  () => book.value?.bids?.[0]?.price ?? currentCommodity.value?.bestBid ?? null,
)
const bestAsk = computed(
  () => book.value?.asks?.[0]?.price ?? currentCommodity.value?.bestAsk ?? null,
)
const totalVolume = computed(() =>
  commodities.value.reduce((sum, item) => sum + num(item.volume), 0),
)
const totalAmount = computed(() =>
  commodities.value.reduce((sum, item) => sum + num(item.amount), 0),
)
const upCount = computed(
  () => commodities.value.filter((item) => num(item.changePct) > 0).length,
)
const downCount = computed(
  () => commodities.value.filter((item) => num(item.changePct) < 0).length,
)
const clockText = computed(() =>
  now.value.toLocaleTimeString('zh-CN', { hour12: false }),
)

const wait = (ms) => new Promise((resolve) => setTimeout(resolve, ms))

const loadFirms = async () => {
  firms.value = await api.firms()
  if (!firms.value.some((item) => item.id === firmId.value) && firms.value.length) {
    firmId.value = firms.value[0].id
  }
}

const loadCommodities = async () => {
  commodities.value = await api.commodities()
  if (!currentId.value && commodities.value.length) {
    currentId.value = commodities.value[0].id
  }
}

const loadMarket = async () => {
  if (!currentId.value) return
  const [bookData, trades, candleData, statusData] = await Promise.all([
    api.book(currentId.value, 10),
    api.marketTrades(currentId.value, 50),
    api.candles(currentId.value, 60),
    api.status(),
  ])
  book.value = bookData
  marketTrades.value = trades
  candles.value = candleData
  engine.value = statusData
}

const loadAccount = async () => {
  if (!firmId.value) return
  const [fundsData, positionData, orderData, tradeData, flowData] = await Promise.all([
    api.funds(firmId.value),
    api.positions(firmId.value),
    api.orders(firmId.value),
    api.myTrades(firmId.value),
    api.flows(firmId.value),
  ])
  funds.value = fundsData
  positions.value = positionData
  orders.value = orderData
  myTrades.value = tradeData
  flows.value = flowData
}

const subscribe = (commodityId) => {
  if (socket && socket.readyState === WebSocket.OPEN && commodityId) {
    socket.send(JSON.stringify({ type: 'subscribe', commodityId }))
  }
}

const handleSocketMessage = (message) => {
  if (!message || message.commodityId !== currentId.value) return
  if (message.type === 'book') {
    book.value = {
      bids: message.bids || [],
      asks: message.asks || [],
      lastPrice: lastPrice.value,
      timestamp: message.timestamp,
    }
  }
  if (message.type === 'trade') {
    const exists = marketTrades.value.some((item) => item.id === message.tradeId)
    if (!exists) {
      marketTrades.value = [
        {
          id: message.tradeId,
          commodityId: message.commodityId,
          price: message.price,
          quantity: message.quantity,
          amount: message.amount,
          side: message.side,
          createTime: message.time,
        },
        ...marketTrades.value,
      ].slice(0, 50)
    }
    window.clearTimeout(accountRefreshTimer)
    accountRefreshTimer = window.setTimeout(() => {
      loadAccount().catch(() => {})
    }, 400)
  }
}

const connectSocket = () => {
  if (destroyed) return
  const protocol = window.location.protocol === 'https:' ? 'wss' : 'ws'
  try {
    socket = new WebSocket(`${protocol}://${window.location.host}/ws/market`)
  } catch (error) {
    scheduleReconnect()
    return
  }
  socket.onopen = () => {
    wsConnected.value = true
    subscribe(currentId.value)
  }
  socket.onclose = () => {
    wsConnected.value = false
    scheduleReconnect()
  }
  socket.onerror = () => {
    wsConnected.value = false
  }
  socket.onmessage = (event) => {
    try {
      handleSocketMessage(JSON.parse(event.data))
    } catch (error) {
      // ignore malformed frames
    }
  }
}

const scheduleReconnect = () => {
  if (destroyed) return
  window.clearTimeout(reconnectTimer)
  reconnectTimer = window.setTimeout(connectSocket, 3000)
}

const selectCommodity = async (commodityId) => {
  currentId.value = commodityId
  book.value = { bids: [], asks: [] }
  marketTrades.value = []
  candles.value = []
  await loadMarket().catch(() => {})
  subscribe(commodityId)
}

const placeOrder = async (payload) => {
  if (!currentId.value) return
  try {
    await api.place(firmId.value, { commodityId: currentId.value, ...payload })
    ElMessage.success('委托已提交')
    await wait(350)
    await Promise.all([loadCommodities(), loadMarket(), loadAccount()])
  } catch (error) {
    ElMessage.error(error.message)
  }
}

const cancelOrder = async (row) => {
  try {
    await api.cancel(firmId.value, row.id)
    ElMessage.success('撤单成功')
    await wait(250)
    await Promise.all([loadMarket(), loadAccount()])
  } catch (error) {
    ElMessage.error(error.message)
  }
}

const resetDemo = async () => {
  try {
    await ElMessageBox.confirm(
      '将清空所有委托、成交、持仓与资金流水，并恢复初始演示数据。是否继续？',
      '重置演示数据',
      { type: 'warning', confirmButtonText: '重置', cancelButtonText: '取消' },
    )
  } catch (error) {
    return
  }
  resetting.value = true
  try {
    await api.reset()
    await loadCommodities()
    if (!commodities.value.some((item) => item.id === currentId.value)) {
      currentId.value = commodities.value[0]?.id ?? null
    }
    await Promise.all([loadMarket(), loadAccount()])
    ElMessage.success('演示数据已重置')
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    resetting.value = false
  }
}

const pickPrice = (price) => {
  orderFormRef.value?.setPrice(price)
}

watch(firmId, async (value) => {
  localStorage.setItem('clearhub.firmId', String(value))
  await loadAccount().catch(() => {})
})

onMounted(async () => {
  try {
    await loadFirms()
    await loadCommodities()
    if (commodities.value.length) {
      currentId.value = commodities.value[0].id
    }
    await Promise.all([loadMarket(), loadAccount()])
  } catch (error) {
    ElMessage.error(`初始化失败：${error.message}`)
  }
  connectSocket()
  clockTimer = window.setInterval(() => {
    now.value = new Date()
  }, 1000)
  marketTimer = window.setInterval(() => {
    loadCommodities().catch(() => {})
    loadMarket().catch(() => {})
  }, 3000)
  accountTimer = window.setInterval(() => {
    loadAccount().catch(() => {})
  }, 4000)
})

onBeforeUnmount(() => {
  destroyed = true
  window.clearInterval(marketTimer)
  window.clearInterval(accountTimer)
  window.clearInterval(clockTimer)
  window.clearTimeout(reconnectTimer)
  window.clearTimeout(accountRefreshTimer)
  socket?.close()
})
</script>

<template>
  <div class="app-shell">
    <header class="topbar">
      <div class="brand">
        <span class="live-badge"><i class="live-dot"></i>LIVE</span>
        ClearHub
        <span>交易核心演示</span>
      </div>
      <div class="view-switch">
        <button :class="{ active: view === 'trade' }" @click="view = 'trade'">交易终端</button>
        <button :class="{ active: view === 'clearing' }" @click="view = 'clearing'">清算与银行</button>
      </div>
      <div class="topbar-right">
        <div class="metric">
          <label>交易账户</label>
          <el-select v-model="firmId" size="small" style="width: 150px">
            <el-option v-for="firm in firms" :key="firm.id" :label="firm.name" :value="firm.id" />
          </el-select>
        </div>
        <div class="metric">
          <label>可用资金</label>
          <strong>{{ fmt(funds.available) }}</strong>
        </div>
        <div class="metric">
          <label>冻结资金</label>
          <strong class="down">{{ fmt(funds.frozenFunds) }}</strong>
        </div>
        <div class="metric">
          <label>账户余额</label>
          <strong>{{ fmt(funds.balance) }}</strong>
        </div>
        <el-button size="small" :loading="resetting" @click="resetDemo">重置演示数据</el-button>
      </div>
    </header>

    <template v-if="view === 'trade'">
      <div class="market-pulse">
        <span class="pulse-item"><i class="pulse-dot"></i> 交易中</span>
        <span class="pulse-item">总成交额 <b>{{ fmt(totalAmount, 0) }}</b></span>
        <span class="pulse-item">总成交量 <b>{{ fmtQty(totalVolume) }}</b></span>
        <span class="pulse-item">上涨 <b class="up">{{ upCount }}</b> · 下跌 <b class="down">{{ downCount }}</b></span>
        <span class="pulse-item">行情源 <b>撮合引擎 + MySQL</b></span>
        <span class="pulse-item">系统时间 <b>{{ clockText }}</b></span>
      </div>
      <main class="layout">
      <div class="left-column">
        <MarketList :commodities="commodities" :current-id="currentId" @select="selectCommodity" />
        <div class="panel engine-card">
          <div class="panel-head">
            <span>撮合引擎</span>
            <span class="engine-state"><i class="live-dot"></i>运行中</span>
          </div>
          <div class="engine-grid">
            <div class="engine-cell">
              <label>累计委托</label>
              <strong>{{ engine?.orders ?? '--' }}</strong>
            </div>
            <div class="engine-cell">
              <label>累计成交</label>
              <strong>{{ engine?.trades ?? '--' }}</strong>
            </div>
            <div class="engine-cell">
              <label>未完成委托</label>
              <strong>{{ engine?.openOrders ?? '--' }}</strong>
            </div>
            <div class="engine-cell">
              <label>账户 / 商品</label>
              <strong>{{ engine?.firms ?? '--' }} / {{ engine?.commodities ?? '--' }}</strong>
            </div>
          </div>
          <div class="engine-rules">
            <span>价格优先</span>
            <span>时间优先</span>
            <span>被动方定价</span>
          </div>
          <div class="engine-foot">内存订单簿 · MySQL 持久化 · WebSocket 推送</div>
        </div>
      </div>

      <section class="center">
        <div class="panel">
          <div class="quote-bar">
            <div class="quote-main">
              <div class="name">{{ currentCommodity?.name || '请选择商品' }}</div>
              <div class="code">{{ currentCommodity?.code || '--' }}</div>
              <div class="quote-price" :class="changeClass(changePct)">
                {{ fmt(lastPrice) }}
              </div>
              <div class="quote-change" :class="changeClass(changePct)">
                {{ num(changePct) >= 0 ? '+' : '' }}{{ fmt(changePct) }}%
              </div>
            </div>
            <div class="quote-cell">
              <label>今开</label>
              <strong>{{ fmt(currentCommodity?.openPrice) }}</strong>
            </div>
            <div class="quote-cell">
              <label>最高</label>
              <strong>{{ fmt(currentCommodity?.highPrice) }}</strong>
            </div>
            <div class="quote-cell">
              <label>最低</label>
              <strong>{{ fmt(currentCommodity?.lowPrice) }}</strong>
            </div>
            <div class="quote-cell">
              <label>成交额</label>
              <strong>{{ fmt(currentCommodity?.amount, 0) }}</strong>
            </div>
            <div class="quote-cell">
              <label>买一</label>
              <strong class="up">{{ fmt(bestBid) }}</strong>
            </div>
            <div class="quote-cell">
              <label>卖一</label>
              <strong class="down">{{ fmt(bestAsk) }}</strong>
            </div>
          </div>
        </div>

        <PriceChart :candles="candles" :last-price="lastPrice" :commodity="currentCommodity" />

        <div class="book-wrap">
          <OrderBook :book="book" :last-price="lastPrice" @pick="pickPrice" />
          <TradeTape :trades="marketTrades" />
        </div>
      </section>

      <div class="right-column">
        <OrderForm
          ref="orderFormRef"
          :commodity="currentCommodity"
          :funds="funds"
          :position="currentPosition"
          :best-bid="bestBid"
          :best-ask="bestAsk"
          @submit="placeOrder"
        />
        <MarketInfo :commodity="currentCommodity" :book="book" />
      </div>
    </main>

    <section class="bottom">
      <div class="panel">
        <el-tabs v-model="tab">
          <el-tab-pane :label="`我的委托 (${orders.length})`" name="orders">
            <OrdersTable :orders="orders" @cancel="cancelOrder" />
          </el-tab-pane>
          <el-tab-pane :label="`我的成交 (${myTrades.length})`" name="trades">
            <TradesTable :trades="myTrades" />
          </el-tab-pane>
          <el-tab-pane :label="`持仓 (${positions.length})`" name="positions">
            <PositionsTable :positions="positions" />
          </el-tab-pane>
          <el-tab-pane label="资金" name="funds">
            <FundsPanel :funds="funds" :firm-name="firmName" />
          </el-tab-pane>
          <el-tab-pane :label="`资金流水 (${flows.length})`" name="flows">
            <FlowsTable :flows="flows" />
          </el-tab-pane>
        </el-tabs>
      </div>
    </section>
    </template>

    <ClearingView v-else :firms="firms" :current-firm-id="firmId" />

    <footer class="footer">
      <span>
        <span
          class="ws-dot"
          :style="{ background: wsConnected ? 'var(--down)' : 'var(--warn)' }"
        ></span>
        {{ wsConnected ? '实时推送已连接' : '实时推送重连中，已启用轮询兜底' }}
        · {{ view === 'trade' ? '模拟做市商 + 模拟交易员持续成交' : '清算所适配器 + 模拟银行' }}
      </span>
      <span class="footer-mid">撮合引擎 · MySQL · 实时行情</span>
      <span>
        演示环境 · 非生产系统 ·
        <a href="/swagger-ui.html" target="_blank" style="color: var(--accent)">API 文档</a>
      </span>
    </footer>
  </div>
</template>

<style scoped>
.ws-dot {
  display: inline-block;
  width: 7px;
  height: 7px;
  border-radius: 50%;
  margin-right: 6px;
  vertical-align: middle;
}

a {
  text-decoration: none;
}
</style>
