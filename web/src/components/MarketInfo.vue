<script setup>
import { computed, nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'
import { fmt, fmtQty, num } from '../utils'

const props = defineProps({
  commodity: { type: Object, default: null },
  book: { type: Object, default: () => ({ bids: [], asks: [] }) },
})

const chartEl = ref(null)
let chart = null
let observer = null

const bids = computed(() => props.book?.bids || [])
const asks = computed(() => props.book?.asks || [])
const bestBid = computed(() => num(bids.value[0]?.price) || num(props.commodity?.bestBid))
const bestAsk = computed(() => num(asks.value[0]?.price) || num(props.commodity?.bestAsk))
const mid = computed(() => {
  if (bestBid.value && bestAsk.value) return (bestBid.value + bestAsk.value) / 2
  return num(props.commodity?.lastPrice)
})
const spread = computed(() => (bestBid.value && bestAsk.value ? bestAsk.value - bestBid.value : 0))
const spreadPct = computed(() => (mid.value ? (spread.value / mid.value) * 100 : 0))
const bidDepth = computed(() => bids.value.reduce((sum, level) => sum + num(level.quantity), 0))
const askDepth = computed(() => asks.value.reduce((sum, level) => sum + num(level.quantity), 0))
const bidRatio = computed(() => {
  const total = bidDepth.value + askDepth.value
  return total ? (bidDepth.value / total) * 100 : 50
})

const info = computed(() => [
  { label: '今开', value: fmt(props.commodity?.openPrice) },
  { label: '最高', value: fmt(props.commodity?.highPrice) },
  { label: '最低', value: fmt(props.commodity?.lowPrice) },
  { label: '成交量', value: fmtQty(props.commodity?.volume) },
  { label: '成交额', value: fmt(props.commodity?.amount, 0) },
  { label: '价差', value: fmt(spread.value) },
  { label: '买盘深度', value: fmtQty(bidDepth.value) },
  { label: '卖盘深度', value: fmtQty(askDepth.value) },
])

const render = () => {
  if (!chart) return
  const bidPoints = []
  let bidSum = 0
  for (const level of bids.value) {
    bidSum += num(level.quantity)
    bidPoints.push([num(level.price), bidSum])
  }
  bidPoints.reverse()
  const askPoints = []
  let askSum = 0
  for (const level of asks.value) {
    askSum += num(level.quantity)
    askPoints.push([num(level.price), askSum])
  }
  if (!bidPoints.length && !askPoints.length) {
    chart.clear()
    chart.setOption({
      backgroundColor: 'transparent',
      title: {
        text: '等待盘口报价',
        left: 'center',
        top: 'middle',
        textStyle: { color: '#7f8ea6', fontSize: 11, fontWeight: 400 },
      },
    })
    return
  }
  const allPrices = [...bidPoints, ...askPoints].map((point) => point[0])
  const min = Math.min(...allPrices)
  const max = Math.max(...allPrices)
  const padding = Math.max((max - min) * 0.08, 0.01)
  chart.setOption(
    {
      backgroundColor: 'transparent',
      animation: true,
      animationDuration: 260,
      grid: { left: 8, right: 8, top: 10, bottom: 16, containLabel: true },
      tooltip: {
        trigger: 'axis',
        backgroundColor: '#111a2b',
        borderColor: '#1e2b40',
        textStyle: { color: '#dbe5f3', fontSize: 11 },
        formatter: (params) => {
          const item = params[0]
          return `${item.seriesName}<br/>价格 ${fmt(item.value[0])}<br/>累计 ${fmtQty(item.value[1])}`
        },
      },
      xAxis: {
        type: 'value',
        min: +(min - padding).toFixed(2),
        max: +(max + padding).toFixed(2),
        axisLabel: { color: '#7f8ea6', fontSize: 9 },
        splitLine: { show: false },
        axisLine: { lineStyle: { color: '#1e2b40' } },
      },
      yAxis: {
        type: 'value',
        axisLabel: { show: false },
        splitLine: { lineStyle: { color: 'rgba(30, 43, 64, 0.5)' } },
        axisLine: { show: false },
      },
      series: [
        {
          name: '买盘',
          type: 'line',
          step: 'end',
          data: bidPoints,
          showSymbol: false,
          lineStyle: { color: '#ff5a5f', width: 1.5 },
          areaStyle: { color: 'rgba(255, 90, 95, 0.18)' },
        },
        {
          name: '卖盘',
          type: 'line',
          step: 'end',
          data: askPoints,
          showSymbol: false,
          lineStyle: { color: '#21c47a', width: 1.5 },
          areaStyle: { color: 'rgba(33, 196, 122, 0.18)' },
        },
      ],
    },
    true,
  )
}

onMounted(async () => {
  await nextTick()
  chart = echarts.init(chartEl.value)
  render()
  observer = new ResizeObserver(() => chart?.resize())
  observer.observe(chartEl.value)
})

watch(() => props.book, render, { deep: true })

onBeforeUnmount(() => {
  observer?.disconnect()
  chart?.dispose()
  chart = null
})
</script>

<template>
  <div class="panel market-info">
    <div class="panel-head">
      <span>市场信息</span>
      <span>{{ commodity?.code || '--' }}</span>
    </div>
    <div class="info-grid">
      <div v-for="item in info" :key="item.label" class="info-cell">
        <label>{{ item.label }}</label>
        <strong>{{ item.value }}</strong>
      </div>
    </div>
    <div class="depth-ratio">
      <span class="up">买 {{ bidRatio.toFixed(0) }}%</span>
      <div class="ratio-bar"><i :style="{ width: `${bidRatio}%` }"></i></div>
      <span class="down">卖 {{ (100 - bidRatio).toFixed(0) }}%</span>
    </div>
    <div ref="chartEl" class="depth-chart"></div>
    <div class="depth-foot">
      <span>价差 {{ fmt(spread) }}（{{ spreadPct.toFixed(2) }}%）</span>
      <span>中间价 {{ fmt(mid) }}</span>
    </div>
  </div>
</template>
