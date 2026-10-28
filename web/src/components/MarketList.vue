<script setup>
import { changeClass, fmt, fmtQty, num } from '../utils'

const props = defineProps({
  commodities: { type: Array, default: () => [] },
  currentId: { type: Number, default: null },
})

const emit = defineEmits(['select'])

const select = (id) => emit('select', id)
</script>

<template>
  <div class="panel">
    <div class="panel-head">
      <span>市场</span>
      <span>{{ commodities.length }} 个商品</span>
    </div>
    <div class="market-list">
      <div
        v-for="item in commodities"
        :key="item.id"
        class="market-item"
        :class="{ active: item.id === currentId }"
        @click="select(item.id)"
      >
        <div class="row">
          <span class="name">{{ item.name }}</span>
          <span class="code">{{ item.code }}</span>
        </div>
        <div class="row">
          <span class="price">{{ fmt(item.lastPrice) }}</span>
          <span :class="changeClass(item.changePct)">
            {{ num(item.changePct) >= 0 ? '+' : '' }}{{ fmt(item.changePct) }}%
          </span>
        </div>
        <div class="sub">
          <span>量 {{ fmtQty(item.volume) }}</span>
          <span>额 {{ fmt(item.amount, 0) }}</span>
        </div>
      </div>
      <div v-if="!commodities.length" class="empty">暂无商品</div>
    </div>
  </div>
</template>
