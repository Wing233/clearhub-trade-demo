<script setup>
import { nextTick, onBeforeUnmount, onMounted, ref, watch } from 'vue'
import * as echarts from 'echarts'
import { num, timeText } from '../utils'

const props = defineProps({
  trades: { type: Array, default: () => [] },
  lastPrice: { type: [String, Number], default: 0 },
})

const container = ref(null)
let chart = null
let observer = null

const render = () => {
  if (!chart) return
  const list = [...props.trades].reverse().slice(-60)
  const prices = list.map((trade) => num(trade.price))
  const categories = list.map((trade) => timeText(trade.createTime))
  if (!prices.length) {
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
  const min = Math.min(...prices)
  const max = Math.max(...prices)
  const padding = Math.max((max - min) * 0.25, max * 0.001, 0.01)
  chart.setOption(
    {
      backgroundColor: 'transparent',
      grid: { left: 10, right: 14, top: 18, bottom: 22, containLabel: true },
      tooltip: {
        trigger: 'axis',
        backgroundColor: '#111a2b',
        borderColor: '#1e2b40',
        textStyle: { color: '#dbe5f3', fontSize: 12 },
        axisPointer: { lineStyle: { color: '#4f8cff' } },
      },
      xAxis: {
        type: 'category',
        data: categories,
        boundaryGap: false,
        axisLine: { lineStyle: { color: '#1e2b40' } },
        axisLabel: { color: '#7f8ea6', fontSize: 10, interval: Math.max(0, Math.floor(categories.length / 6) - 1) },
        splitLine: { show: false },
      },
      yAxis: {
        type: 'value',
        min: +(min - padding).toFixed(2),
        max: +(max + padding).toFixed(2),
        axisLabel: { color: '#7f8ea6', fontSize: 10 },
        splitLine: { lineStyle: { color: 'rgba(30, 43, 64, 0.6)' } },
      },
      series: [
        {
          type: 'line',
          data: prices,
          smooth: true,
          showSymbol: prices.length <= 3,
          symbolSize: 6,
          lineStyle: { color: '#4f8cff', width: 2 },
          areaStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
              { offset: 0, color: 'rgba(79, 140, 255, 0.28)' },
              { offset: 1, color: 'rgba(79, 140, 255, 0.02)' },
            ]),
          },
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

watch(() => props.trades, render, { deep: true })

onBeforeUnmount(() => {
  observer?.disconnect()
  chart?.dispose()
  chart = null
})
</script>

<template>
  <div class="panel">
    <div class="panel-head">
      <span>价格走势</span>
      <span>最近 60 笔成交</span>
    </div>
    <div ref="container" class="chart-box"></div>
  </div>
</template>
