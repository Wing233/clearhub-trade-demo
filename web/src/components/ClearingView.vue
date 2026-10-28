<script setup>
import { onMounted, ref, watch } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { api } from '../api'
import { changeClass, dateTimeText, fmt, signText, timeText } from '../utils'

const props = defineProps({
  firms: { type: Array, default: () => [] },
  currentFirmId: { type: Number, default: null },
})

const summary = ref({
  unsettledTrades: 0,
  unsettledAmount: 0,
  lastBatchNo: '',
  lastBatchTime: null,
  bankTotal: 0,
  ledgerTotal: 0,
})
const batches = ref([])
const selectedBatch = ref(null)
const accounts = ref([])
const transactions = ref([])
const loading = ref(false)
const running = ref(false)
const dialogVisible = ref(false)
const dialogType = ref('DEPOSIT')
const dialogAccount = ref(null)
const dialogAmount = ref(1000)
const dialogRemark = ref('演示出入金')
const submitting = ref(false)

const loadSummary = async () => {
  summary.value = await api.clearingSummary()
}

const loadBatches = async () => {
  batches.value = await api.clearingBatches(20)
  if (batches.value.length && !selectedBatch.value) {
    await selectBatch(batches.value[0])
  }
}

const loadAccounts = async () => {
  accounts.value = await api.bankAccounts()
}

const loadTransactions = async () => {
  transactions.value = await api.bankTransactions(null, 100)
}

const loadAll = async () => {
  loading.value = true
  try {
    await Promise.all([loadSummary(), loadBatches(), loadAccounts(), loadTransactions()])
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loading.value = false
  }
}

const selectBatch = async (row) => {
  try {
    selectedBatch.value = await api.clearingBatch(row.id)
  } catch (error) {
    ElMessage.error(error.message)
  }
}

const runClearing = async () => {
  try {
    await ElMessageBox.confirm(
      '将把当前未清算成交归集为一个批次，并按内部资金余额同步银行结算账户。是否继续？',
      '执行日终清算',
      { type: 'warning', confirmButtonText: '执行', cancelButtonText: '取消' },
    )
  } catch (error) {
    return
  }
  running.value = true
  try {
    const batch = await api.clearingRun()
    ElMessage.success(`清算完成：${batch.batchNo}，成交 ${batch.totalTrades} 笔`)
    selectedBatch.value = batch
    await Promise.all([loadSummary(), loadBatches(), loadAccounts(), loadTransactions()])
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    running.value = false
  }
}

const openDialog = (type, account) => {
  dialogType.value = type
  dialogAccount.value = account
  dialogAmount.value = 1000
  dialogRemark.value = type === 'DEPOSIT' ? '演示入金' : '演示出金'
  dialogVisible.value = true
}

const submitBank = async () => {
  if (!dialogAccount.value) return
  submitting.value = true
  try {
    const payload = {
      firmId: dialogAccount.value.firmId,
      amount: dialogAmount.value,
      remark: dialogRemark.value,
    }
    if (dialogType.value === 'DEPOSIT') {
      await api.bankDeposit(payload)
      ElMessage.success('入金成功')
    } else {
      await api.bankWithdraw(payload)
      ElMessage.success('出金成功')
    }
    dialogVisible.value = false
    await Promise.all([loadSummary(), loadAccounts(), loadTransactions()])
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    submitting.value = false
  }
}

const statusType = (status) => (status === 'MATCH' ? 'success' : 'warning')
const typeType = (type) => {
  if (type === 'DEPOSIT') return 'success'
  if (type === 'WITHDRAW') return 'warning'
  return 'info'
}

watch(
  () => props.currentFirmId,
  () => loadTransactions().catch(() => {}),
)

onMounted(loadAll)
</script>

<template>
  <section class="clearing-view" v-loading="loading">
    <div class="panel clearing-hero">
      <div>
        <h2>清算与银行</h2>
        <p>成交归集 → 日终清算 → 银行结算账户对账 / 出入金</p>
      </div>
      <div class="hero-actions">
        <el-button size="small" @click="loadAll">刷新</el-button>
        <el-button
          type="primary"
          size="small"
          :loading="running"
          :disabled="summary.unsettledTrades === 0"
          @click="runClearing"
        >
          执行日终清算
        </el-button>
      </div>
    </div>

    <div class="clearing-cards">
      <div class="clear-card">
        <label>未清算成交</label>
        <strong>{{ summary.unsettledTrades }} 笔</strong>
      </div>
      <div class="clear-card">
        <label>未清算金额</label>
        <strong>{{ fmt(summary.unsettledAmount) }}</strong>
      </div>
      <div class="clear-card">
        <label>最近批次</label>
        <strong>{{ summary.lastBatchNo || '--' }}</strong>
        <span>{{ dateTimeText(summary.lastBatchTime) }}</span>
      </div>
      <div class="clear-card">
        <label>银行结算账户合计</label>
        <strong>{{ fmt(summary.bankTotal) }}</strong>
        <span>内部资金合计 {{ fmt(summary.ledgerTotal) }}</span>
      </div>
    </div>

    <div class="clearing-grid">
      <div class="panel">
        <div class="panel-head">
          <span>清算批次</span>
          <span>点击查看明细</span>
        </div>
        <el-table
          :data="batches"
          max-height="300"
          empty-text="暂无清算批次"
          highlight-current-row
          @row-click="selectBatch"
        >
          <el-table-column label="批次号" min-width="190" prop="batchNo" />
          <el-table-column label="交易日" width="100" prop="tradeDay" />
          <el-table-column label="成交" width="70" align="right" prop="totalTrades" />
          <el-table-column label="金额" width="120" align="right">
            <template #default="{ row }">{{ fmt(row.totalAmount) }}</template>
          </el-table-column>
          <el-table-column label="状态" width="90">
            <template #default="{ row }">
              <el-tag size="small" type="success" effect="dark">{{ row.status }}</el-tag>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="panel">
        <div class="panel-head">
          <span>批次明细</span>
          <span>{{ selectedBatch?.batchNo || '--' }}</span>
        </div>
        <template v-if="selectedBatch">
          <el-table :data="selectedBatch.details" max-height="220" empty-text="该批次无成交明细">
            <el-table-column label="账户" min-width="110">
              <template #default="{ row }">{{ row.firmName }}</template>
            </el-table-column>
            <el-table-column label="商品" min-width="100">
              <template #default="{ row }">{{ row.commodityCode }}</template>
            </el-table-column>
            <el-table-column label="期初" width="100" align="right">
              <template #default="{ row }">{{ fmt(row.openingQty, 0) }}</template>
            </el-table-column>
            <el-table-column label="买入" width="90" align="right">
              <template #default="{ row }">{{ fmt(row.buyQty, 0) }}</template>
            </el-table-column>
            <el-table-column label="卖出" width="90" align="right">
              <template #default="{ row }">{{ fmt(row.sellQty, 0) }}</template>
            </el-table-column>
            <el-table-column label="期末" width="100" align="right">
              <template #default="{ row }">{{ fmt(row.closingQty, 0) }}</template>
            </el-table-column>
            <el-table-column label="手续费" width="90" align="right">
              <template #default="{ row }">{{ fmt(row.fee) }}</template>
            </el-table-column>
            <el-table-column label="净额" width="110" align="right">
              <template #default="{ row }">
                <span :class="changeClass(row.netAmount)">{{ signText(row.netAmount) }}</span>
              </template>
            </el-table-column>
          </el-table>

          <div class="panel-head sub-head">
            <span>对账结果</span>
            <span>清算前银行余额 → 结算后</span>
          </div>
          <el-table :data="selectedBatch.reconciliations" max-height="220" empty-text="无对账记录">
            <el-table-column label="账户" min-width="110">
              <template #default="{ row }">{{ row.firmName }}</template>
            </el-table-column>
            <el-table-column label="内部余额" width="120" align="right">
              <template #default="{ row }">{{ fmt(row.ledgerBalance) }}</template>
            </el-table-column>
            <el-table-column label="银行余额" width="120" align="right">
              <template #default="{ row }">{{ fmt(row.bankBalance) }}</template>
            </el-table-column>
            <el-table-column label="结算金额" width="120" align="right">
              <template #default="{ row }">
                <span :class="changeClass(row.settledAmount)">{{ signText(row.settledAmount) }}</span>
              </template>
            </el-table-column>
            <el-table-column label="结果" width="90">
              <template #default="{ row }">
                <el-tag size="small" :type="statusType(row.status)" effect="dark">{{ row.status }}</el-tag>
              </template>
            </el-table-column>
          </el-table>
        </template>
        <div v-else class="empty">选择左侧批次查看明细</div>
      </div>
    </div>

    <div class="panel">
      <div class="panel-head">
        <span>银行结算账户</span>
        <span>入金 / 出金会同步内部资金账户</span>
      </div>
      <el-table :data="accounts" max-height="240" empty-text="暂无银行账户">
        <el-table-column label="账户" min-width="120">
          <template #default="{ row }">{{ row.firmName }}</template>
        </el-table-column>
        <el-table-column label="账号" min-width="140" prop="accountNo" />
        <el-table-column label="开户行" min-width="140" prop="bankName" />
        <el-table-column label="余额" width="150" align="right">
          <template #default="{ row }">{{ fmt(row.balance) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="160" align="center">
          <template #default="{ row }">
            <el-button link type="success" size="small" @click="openDialog('DEPOSIT', row)">入金</el-button>
            <el-button link type="warning" size="small" @click="openDialog('WITHDRAW', row)">出金</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>

    <div class="panel">
      <div class="panel-head">
        <span>银行流水</span>
        <span>入金 / 出金 / 清算结算</span>
      </div>
      <el-table :data="transactions" max-height="240" empty-text="暂无银行流水">
        <el-table-column label="时间" width="160">
          <template #default="{ row }">{{ dateTimeText(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="账户" min-width="110">
          <template #default="{ row }">{{ row.firmName }}</template>
        </el-table-column>
        <el-table-column label="类型" width="100">
          <template #default="{ row }">
            <el-tag size="small" :type="typeType(row.type)" effect="plain">{{ row.typeLabel }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="金额" width="140" align="right">
          <template #default="{ row }">
            <span :class="changeClass(row.amount)">{{ signText(row.amount) }}</span>
          </template>
        </el-table-column>
        <el-table-column label="余额" width="140" align="right">
          <template #default="{ row }">{{ fmt(row.balanceAfter) }}</template>
        </el-table-column>
        <el-table-column label="备注" min-width="180" prop="remark" />
      </el-table>
    </div>

    <el-dialog
      v-model="dialogVisible"
      :title="dialogType === 'DEPOSIT' ? '银行入金' : '银行出金'"
      width="420px"
    >
      <el-form label-position="top">
        <el-form-item label="账户">
          <el-input :model-value="dialogAccount?.firmName" disabled />
        </el-form-item>
        <el-form-item label="金额">
          <el-input-number v-model="dialogAmount" :min="0.01" :precision="2" style="width: 100%" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model="dialogRemark" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitBank">确认</el-button>
      </template>
    </el-dialog>
  </section>
</template>
