<script setup>
import { fmt } from '../utils'

defineProps({
  funds: { type: Object, default: () => ({ balance: 0, frozenFunds: 0, available: 0 }) },
  firmName: { type: String, default: '' },
})
</script>

<template>
  <div class="funds-panel">
    <div class="fund-card">
      <label>账户余额</label>
      <strong>{{ fmt(funds.balance) }}</strong>
      <span>{{ firmName }}</span>
    </div>
    <div class="fund-card">
      <label>冻结资金</label>
      <strong class="down">{{ fmt(funds.frozenFunds) }}</strong>
      <span>挂单占用</span>
    </div>
    <div class="fund-card">
      <label>可用资金</label>
      <strong class="up">{{ fmt(funds.available) }}</strong>
      <span>可继续下单</span>
    </div>
    <div class="fund-card muted-card">
      <label>演示说明</label>
      <span>
        手续费率 {{ (0.0005 * 100).toFixed(3) }}%，买卖双向收取；成交后资金与持仓实时更新，可在下方「资金流水」查看每一笔变动。
      </span>
    </div>
  </div>
</template>

<style scoped>
.funds-panel {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px;
  padding: 8px 4px 4px;
}

.fund-card {
  display: flex;
  flex-direction: column;
  gap: 6px;
  padding: 14px 16px;
  border: 1px solid var(--line);
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.02);
}

.fund-card label {
  color: var(--muted);
  font-size: 11px;
  letter-spacing: 0.6px;
}

.fund-card strong {
  font-size: 22px;
  font-variant-numeric: tabular-nums;
}

.fund-card span {
  color: var(--muted);
  font-size: 11px;
  line-height: 1.6;
}

.muted-card {
  justify-content: center;
}

@media (max-width: 1100px) {
  .funds-panel {
    grid-template-columns: 1fr 1fr;
  }
}
</style>
