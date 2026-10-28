<script setup>
import { fmt, fmtQty, timeText } from '../utils'

defineProps({
  orders: { type: Array, default: () => [] },
})

const emit = defineEmits(['cancel'])

const statusType = (status) => {
  switch (status) {
    case 0:
    case 1:
      return 'warning'
    case 2:
      return 'success'
    case 3:
      return 'info'
    default:
      return 'danger'
  }
}

const cancellable = (order) => order.status === 0 || order.status === 1
</script>

<template>
  <el-table :data="orders" max-height="280" empty-text="暂无委托">
    <el-table-column label="时间" width="96">
      <template #default="{ row }">{{ timeText(row.createTime) }}</template>
    </el-table-column>
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
    <el-table-column label="已成交" width="100" align="right">
      <template #default="{ row }">{{ fmtQty(row.filledQuantity) }}</template>
    </el-table-column>
    <el-table-column label="状态" width="96">
      <template #default="{ row }">
        <el-tag size="small" :type="statusType(row.status)" effect="dark">{{ row.statusLabel }}</el-tag>
      </template>
    </el-table-column>
    <el-table-column label="操作" width="86" align="center">
      <template #default="{ row }">
        <el-button
          v-if="cancellable(row)"
          link
          type="danger"
          size="small"
          @click="emit('cancel', row)"
        >
          撤单
        </el-button>
        <span v-else class="muted">--</span>
      </template>
    </el-table-column>
  </el-table>
</template>
