<script setup>
import { changeClass, dateTimeText, fmt, signText } from '../utils'

defineProps({
  flows: { type: Array, default: () => [] },
})
</script>

<template>
  <el-table :data="flows" max-height="280" empty-text="暂无资金流水">
    <el-table-column label="时间" width="160">
      <template #default="{ row }">{{ dateTimeText(row.createTime) }}</template>
    </el-table-column>
    <el-table-column label="业务" width="110" prop="bizLabel" />
    <el-table-column label="资金变动" width="130" align="right">
      <template #default="{ row }">
        <span :class="changeClass(row.amount)">{{ signText(row.amount) }}</span>
      </template>
    </el-table-column>
    <el-table-column label="冻结变动" width="130" align="right">
      <template #default="{ row }">
        <span :class="changeClass(row.frozenAmount)">{{ signText(row.frozenAmount) }}</span>
      </template>
    </el-table-column>
    <el-table-column label="余额" width="130" align="right">
      <template #default="{ row }">{{ fmt(row.balanceAfter) }}</template>
    </el-table-column>
    <el-table-column label="冻结" width="130" align="right">
      <template #default="{ row }">{{ fmt(row.frozenAfter) }}</template>
    </el-table-column>
    <el-table-column label="备注" min-width="160" prop="note" />
  </el-table>
</template>
