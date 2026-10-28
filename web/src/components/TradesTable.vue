<script setup>
import { fmt, fmtQty, timeText } from '../utils'

defineProps({
  trades: { type: Array, default: () => [] },
})
</script>

<template>
  <el-table :data="trades" max-height="280" empty-text="暂无成交">
    <el-table-column label="时间" width="96">
      <template #default="{ row }">{{ timeText(row.createTime) }}</template>
    </el-table-column>
    <el-table-column label="成交号" min-width="180" prop="tradeNo" />
    <el-table-column label="商品" min-width="110">
      <template #default="{ row }">{{ row.commodityName }}</template>
    </el-table-column>
    <el-table-column label="方向" width="76">
      <template #default="{ row }">
        <el-tag size="small" :class="row.side === 'B' ? 'tag-buy' : 'tag-sell'" effect="plain">
          {{ row.sideLabel }}
        </el-tag>
      </template>
    </el-table-column>
    <el-table-column label="价格" width="100" align="right">
      <template #default="{ row }">{{ fmt(row.price) }}</template>
    </el-table-column>
    <el-table-column label="数量" width="100" align="right">
      <template #default="{ row }">{{ fmtQty(row.quantity) }}</template>
    </el-table-column>
    <el-table-column label="金额" width="120" align="right">
      <template #default="{ row }">{{ fmt(row.amount) }}</template>
    </el-table-column>
    <el-table-column label="手续费" width="100" align="right">
      <template #default="{ row }">
        {{ fmt(row.side === 'B' ? row.buyFee : row.sellFee) }}
      </template>
    </el-table-column>
  </el-table>
</template>
