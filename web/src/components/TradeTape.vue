<script setup>
import { computed } from 'vue'
import { fmt, fmtQty, num, timeText } from '../utils'

const props = defineProps({
  trades: { type: Array, default: () => [] },
})

const rows = computed(() => {
  const list = props.trades.slice(0, 14)
  return list.map((trade, index) => {
    const previous = list[index + 1]
    const direction =
      previous === undefined || num(trade.price) === num(previous.price)
        ? 'flat'
        : num(trade.price) > num(previous.price)
          ? 'up'
          : 'down'
    return {
      ...trade,
      direction,
      side: trade.side || trade.takerSide || (direction === 'up' ? 'B' : 'S'),
    }
  })
})
</script>

<template>
  <div class="panel trade-tape">
    <div class="panel-head">
      <span>最近成交</span>
      <span>时间 / 方向 / 价格 / 数量</span>
    </div>
    <div class="tape-row tape-head">
      <span>时间</span>
      <span>方向</span>
      <span>价格</span>
      <span style="text-align: right">数量</span>
    </div>
    <div v-for="trade in rows" :key="trade.id" class="tape-row tape-body">
      <span class="time">{{ timeText(trade.createTime) }}</span>
      <span class="tape-side" :class="trade.side === 'B' ? 'buy' : 'sell'">
        {{ trade.side === 'B' ? '买' : '卖' }}
      </span>
      <span :class="trade.direction">{{ fmt(trade.price) }}</span>
      <span class="qty">{{ fmtQty(trade.quantity) }}</span>
    </div>
    <div v-if="!rows.length" class="empty">暂无成交，模拟交易员正在下单</div>
  </div>
</template>
