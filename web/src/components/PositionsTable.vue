<script setup>
import { changeClass, fmt, fmtQty, signText } from '../utils'

defineProps({
  positions: { type: Array, default: () => [] },
})
</script>

<template>
  <el-table :data="positions" max-height="280" empty-text="暂无持仓">
    <el-table-column label="商品" min-width="140">
      <template #default="{ row }">
        <div>{{ row.commodityName }}</div>
        <div class="muted" style="font-size: 11px">{{ row.commodityCode }}</div>
      </template>
    </el-table-column>
    <el-table-column label="持仓" width="110" align="right">
      <template #default="{ row }">{{ fmtQty(row.quantity) }}</template>
    </el-table-column>
    <el-table-column label="可用" width="110" align="right">
      <template #default="{ row }">{{ fmtQty(row.available) }}</template>
    </el-table-column>
    <el-table-column label="冻结" width="100" align="right">
      <template #default="{ row }">{{ fmtQty(row.frozenQuantity) }}</template>
    </el-table-column>
    <el-table-column label="成本价" width="100" align="right">
      <template #default="{ row }">{{ fmt(row.avgPrice) }}</template>
    </el-table-column>
    <el-table-column label="最新价" width="100" align="right">
      <template #default="{ row }">{{ fmt(row.lastPrice) }}</template>
    </el-table-column>
    <el-table-column label="市值" width="130" align="right">
      <template #default="{ row }">{{ fmt(row.marketValue) }}</template>
    </el-table-column>
    <el-table-column label="浮动盈亏" width="130" align="right">
      <template #default="{ row }">
        <span :class="changeClass(row.pnl)">
          {{ signText(row.pnl) }}（{{ signText(row.pnlPct) }}%）
        </span>
      </template>
    </el-table-column>
  </el-table>
</template>
