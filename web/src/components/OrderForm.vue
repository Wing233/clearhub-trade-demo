<script setup>
import { computed, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { fmt, fmtQty, num, tickPrecision } from '../utils'

const props = defineProps({
  commodity: { type: Object, default: null },
  funds: { type: Object, default: () => ({ available: 0, balance: 0, frozenFunds: 0 }) },
  position: { type: Object, default: null },
  bestBid: { type: [String, Number], default: null },
  bestAsk: { type: [String, Number], default: null },
})

const emit = defineEmits(['submit'])

const side = ref('BUY')
const price = ref(0)
const quantity = ref(1)
const priceTouched = ref(false)

const tick = computed(() => num(props.commodity?.minPriceChange ?? 0.01))
const minQty = computed(() => Math.max(num(props.commodity?.minAmountChange ?? 1), 0.0001))
const pricePrecision = computed(() => tickPrecision(tick.value))
const qtyPrecision = computed(() => tickPrecision(minQty.value))
const feeRate = computed(() => num(props.commodity?.feeRate ?? 0))

const maxBuy = computed(() => {
  if (price.value <= 0) return 0
  return num(props.funds?.available) / (price.value * (1 + feeRate.value))
})
const maxSell = computed(() => num(props.position?.available))
const maxQuantity = computed(() => (side.value === 'BUY' ? maxBuy.value : maxSell.value))

const total = computed(() => price.value * quantity.value)
const fee = computed(() => total.value * feeRate.value)

const resetPrice = () => {
  const fallback = num(props.commodity?.lastPrice)
  const target =
    side.value === 'BUY'
      ? num(props.bestAsk) || fallback
      : num(props.bestBid) || fallback
  price.value = target || tick.value
  quantity.value = minQty.value
  priceTouched.value = false
}

watch(
  () => props.commodity?.id,
  () => resetPrice(),
  { immediate: true },
)

watch(
  () => [props.bestBid, props.bestAsk],
  () => {
    if (priceTouched.value) return
    const fallback = num(props.commodity?.lastPrice)
    const target =
      side.value === 'BUY'
        ? num(props.bestAsk) || fallback
        : num(props.bestBid) || fallback
    if (target) price.value = target
  },
)

const switchSide = (value) => {
  side.value = value
  resetPrice()
}

const setPrice = (value) => {
  price.value = num(value)
  priceTouched.value = true
}

const markPriceTouched = () => {
  priceTouched.value = true
}

const applyPercent = (percent) => {
  const raw = maxQuantity.value * percent
  const steps = Math.floor(raw / minQty.value)
  quantity.value = Math.max(minQty.value, steps * minQty.value)
}

const submit = () => {
  if (!props.commodity) {
    ElMessage.warning('请选择商品')
    return
  }
  if (!price.value || price.value <= 0) {
    ElMessage.warning('请输入有效价格')
    return
  }
  if (!quantity.value || quantity.value <= 0) {
    ElMessage.warning('请输入有效数量')
    return
  }
  emit('submit', {
    side: side.value,
    price: price.value,
    quantity: quantity.value,
  })
}

defineExpose({ setPrice })
</script>

<template>
  <div class="panel order-panel">
    <div class="panel-head">
      <span>下单</span>
      <span>{{ commodity ? commodity.code : '--' }}</span>
    </div>
    <div class="side-switch">
      <button class="side-btn buy" :class="{ active: side === 'BUY' }" @click="switchSide('BUY')">
        买入
      </button>
      <button class="side-btn sell" :class="{ active: side === 'SELL' }" @click="switchSide('SELL')">
        卖出
      </button>
    </div>
    <div class="form-body">
      <div class="field">
        <label>价格（最小变动 {{ tick }}）</label>
        <el-input-number
          v-model="price"
          :min="tick"
          :step="tick"
          :precision="pricePrecision"
          controls-position="right"
          style="width: 100%"
          @change="markPriceTouched"
        />
      </div>
      <div class="field">
        <label>数量（最小变动 {{ minQty }}）</label>
        <el-input-number
          v-model="quantity"
          :min="minQty"
          :step="minQty"
          :precision="qtyPrecision"
          controls-position="right"
          style="width: 100%"
        />
      </div>
      <div class="quick-row">
        <button v-for="p in [0.25, 0.5, 0.75, 1]" :key="p" class="quick-btn" @click="applyPercent(p)">
          {{ p * 100 }}%
        </button>
      </div>
      <div class="summary-row">
        <span>可用资金</span>
        <strong>{{ fmt(funds.available) }}</strong>
      </div>
      <div class="summary-row">
        <span>可用持仓</span>
        <strong>{{ fmtQty(position ? position.available : 0) }}</strong>
      </div>
      <div class="summary-row">
        <span>预计金额</span>
        <strong>{{ fmt(total) }}</strong>
      </div>
      <div class="summary-row">
        <span>预计手续费</span>
        <strong>{{ fmt(fee) }}</strong>
      </div>
      <button class="submit-btn" :class="side === 'BUY' ? 'buy' : 'sell'" @click="submit">
        {{ side === 'BUY' ? '买入' : '卖出' }} {{ commodity ? commodity.name : '' }}
      </button>
      <div class="muted" style="font-size: 11px; line-height: 1.6">
        限价单 · 价格优先 / 时间优先 · 成交价取被动方价格
      </div>
    </div>
  </div>
</template>
