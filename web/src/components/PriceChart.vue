<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'
import { num } from '../utils'

const props = defineProps({
  candles: { type: Array, default: () => [] },
  lastPrice: { type: [String, Number], default: 0 },
  commodity: { type: Object, default: null },
})

const container = ref(null)
const syntheticUsed = ref(false)
let chart = null
let observer = null

const toNum = (value) => Number(value || 0)

const seededRandom = (seed) => {
  let state = seed >>> 0
  return () => {
    state = (state + 0x6d2b79f5) >>> 0
    let t = state
    t = Math.imul(t ^ (t >>> 15), t | 1)
    t ^= t + Math.imul(t ^ (t >>> 7), t | 61)
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296
  }
}

const parseTime = (text) => {
  const [h, m] = String(text || '09:30').split(':').map(Number)
  return (Number.isFinite(h) ? h : 9) * 60 + (Number.isFinite(m) ? m : 30)
}

const formatTime = (minutes) => {
  const h = Math.floor(minutes / 60) % 24
  const m = Math.floor(minutes % 60)
  return `${String(h).padStart(2, '0')}:${String(m).padStart(2, '0')}`
}

const buildSeries = () => {
  const real = (props.candles || []).map((candle) => ({
    time: candle.time,
    open: toNum(candle.open),
    high: toNum(candle.high),
    low: toNum(candle.low),
    close: toNum(candle.close),
    volume: toNum(candle.volume),
    synthetic: false,
  }))
  const need = Math.max(0, 36 - real.length)
  syntheticUsed.value = need > 0
  if (!need) return real

  const base = real.length ? real[0].open : toNum(props.lastPrice) || 100
  const random = seededRandom((props.commodity?.id || 1) * 7919)
  const synthetic = []
  let previous = base * (1 - 0.006 - random() * 0.004)
  const start = (real.length ? parseTime(real[0].time) : 9 * 60 + 30) - need
  for (let i = 0; i < need; i++) {
    const close = Math.max(0.01, previous * (1 + (random() - 0.5) * 0.006))
    const open = previous
    synthetic.push({
      time: formatTime(start + i),
      open,
      high: Math.max(open, close) * (1 + random() * 0.002),
      low: Math.min(open, close) * (1 - random() * 0.002),
      close,
      volume: 30 + random() * 70,
      synthetic: true,
    })
    previous = close
  }
  const scale = base / synthetic[synthetic.length - 1].close
  return synthetic
    .map((candle) => ({
      ...candle,
      open: candle.open * scale,
      high: candle.high * scale,
      low: candle.low * scale,
      close: candle.close * scale,
    }))
    .concat(real)
}

const movingAverage = (list, windowSize) =>
  list.map((_, index) => {
    if (index < windowSize - 1) return null
    let sum = 0
    for (let i = 0; i < windowSize; i++) sum += list[index - i].close
    return +(sum / windowSize).toFixed(2)
  })

const render = () => {
  if (!chart) return
  const list = buildSeries()
  if (!list.length) {
    chart.clear()
    chart.setOption({
      backgroundColor: 'transparent',
      title: {
        text: '等待成交数据',
        left: 'center',
        top: 'middle',
        textStyle: { color: '#7f8ea6', fontSize: 12, fontWeight: 400 },
      },
    })
    return
  }
  const times = list.map((candle) => candle.time)
  const candles = list.map((candle) => [candle.open, candle.close, candle.low, candle.high])
  const volumes = list.map((candle) => ({
    value: candle.volume,
    itemStyle: {
      color: candle.close >= candle.open ? 'rgba(255, 90, 95, 0.55)' : 'rgba(33, 196, 122, 0.55)',
    },
  }))
  chart.setOption(
    {
      backgroundColor: 'transparent',
      animation: true,
      animationDuration: 320,
      legend: {
        top: 0,
        right: 8,
        itemWidth: 10,
        itemHeight: 6,
        textStyle: { color: '#7f8ea6', fontSize: 10 },
        data: ['K线', 'MA5', 'MA10'],
      },
      tooltip: {
        trigger: 'axis',
        axisPointer: { type: 'cross' },
        backgroundColor: '#111a2b',
        borderColor: '#1e2b40',
        textStyle: { color: '#dbe5f3', fontSize: 11 },
      },
      axisPointer: { link: [{ xAxisIndex: 'all' }] },
      grid: [
        { left: 10, right: 14, top: 28, height: '56%', containLabel: true },
        { left: 10, right: 14, top: '72%', height: '16%', containLabel: true },
      ],
      xAxis: [
        {
          type: 'category',
          data: times,
          boundaryGap: true,
          axisLine: { lineStyle: { color: '#1e2b40' } },
          axisLabel: {
            color: '#7f8ea6',
            fontSize: 10,
            interval: Math.max(0, Math.floor(times.length / 6) - 1),
          },
          splitLine: { show: false },
        },
        {
          type: 'category',
          gridIndex: 1,
          data: times,
          boundaryGap: true,
          axisLabel: { show: false },
          axisLine: { lineStyle: { color: '#1e2b40' } },
          axisTick: { show: false },
        },
      ],
      yAxis: [
        {
          scale: true,
          position: 'right',
          axisLabel: { color: '#7f8ea6', fontSize: 10 },
          splitLine: { lineStyle: { color: 'rgba(30, 43, 64, 0.6)' } },
          axisLine: { show: false },
        },
        {
          scale: true,
          gridIndex: 1,
          axisLabel: { show: false },
          splitLine: { show: false },
          axisLine: { show: false },
        },
      ],
      dataZoom: [{ type: 'inside', xAxisIndex: [0, 1], start: 0, end: 100 }],
      series: [
        {
          name: 'K线',
          type: 'candlestick',
          data: candles,
          itemStyle: {
            color: '#ff5a5f',
            color0: '#21c47a',
            borderColor: '#ff5a5f',
            borderColor0: '#21c47a',
          },
        },
        {
          name: 'MA5',
          type: 'line',
          data: movingAverage(list, 5),
          smooth: true,
          showSymbol: false,
          lineStyle: { width: 1, color: '#f6b73c' },
          itemStyle: { color: '#f6b73c' },
        },
        {
          name: 'MA10',
          type: 'line',
          data: movingAverage(list, 10),
          smooth: true,
          showSymbol: false,
          lineStyle: { width: 1, color: '#4f8cff' },
          itemStyle: { color: '#4f8cff' },
        },
        {
          name: '成交量',
          type: 'bar',
          xAxisIndex: 1,
          yAxisIndex: 1,
          data: volumes,
          barWidth: '62%',
        },
      ],
    },
    true,
  )
}

onMounted(async () => {
  await nextTick()
  chart = echarts.init(container.value)
  render()
  observer = new ResizeObserver(() => chart?.resize())
  observer.observe(container.value)
})

watch(() => [props.candles, props.lastPrice], render, { deep: true })

onBeforeUnmount(() => {
  observer?.disconnect()
  chart?.dispose()
  chart = null
})
</script>

<template>
  <div class="panel">
    <div class="panel-head">
      <span>行情走势 · 1 分钟</span>
      <span>
        <em v-if="syntheticUsed" class="chart-badge">含模拟历史</em>
        最近 36 根
      </span>
    </div>
    <div ref="container" class="chart-box"></div>
  </div>
</template>
