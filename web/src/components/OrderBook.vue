<script setup>
import { computed } from 'vue'
import { fmt, fmtQty, num } from '../utils'

const props = defineProps({
  book: { type: Object, default: () => ({ bids: [], asks: [] }) },
  lastPrice: { type: [String, Number], default: 0 },
})

const emit = defineEmits(['pick'])

const asks = computed(() => [...(props.book?.asks || [])].slice(0, 10).reverse())
const bids = computed(() => [...(props.book?.bids || [])].slice(0, 10))
const maxQty = computed(() => {
  const all = [...(props.book?.asks || []), ...(props.book?.bids || [])]
  return Math.max(1, ...all.map((level) => num(level.quantity)))
})

const depthWidth = (quantity) => `${Math.min(100, (num(quantity) / maxQty.value) * 100)}%`
const pick = (level) => emit('pick', level.price)
</script>

<template>
  <div class="panel book">
    <div class="panel-head">
      <span>盘口</span>
      <span>价格 / 数量</span>
    </div>
    <div class="book-row">
      <span>价格</span>
      <span style="text-align: right">数量</span>
    </div>
    <div
      v-for="level in asks"
      :key="`ask-${level.price}`"
      class="level ask"
      @click="pick(level)"
    >
      <span class="depth" :style="{ width: depthWidth(level.quantity) }"></span>
      <span class="price">{{ fmt(level.price) }}</span>
      <span class="qty">{{ fmtQty(level.quantity) }}</span>
    </div>
    <div v-if="!asks.length" class="empty" style="min-height: 60px">等待卖盘报价</div>
    <div class="mid-price">
      <span>最新价</span>
      <strong>{{ fmt(lastPrice) }}</strong>
    </div>
    <div
      v-for="level in bids"
      :key="`bid-${level.price}`"
      class="level bid"
      @click="pick(level)"
    >
      <span class="depth" :style="{ width: depthWidth(level.quantity) }"></span>
      <span class="price">{{ fmt(level.price) }}</span>
      <span class="qty">{{ fmtQty(level.quantity) }}</span>
    </div>
    <div v-if="!bids.length" class="empty" style="min-height: 60px">等待买盘报价</div>
  </div>
</template>
