<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'

import {
  getPayoutPage,
  PAYOUT_CHANNEL_MAP,
  PAYOUT_STATUS_MAP,
  payoutChannelText,
  payoutStatusInfo,
  type PayoutOrderVO,
} from '@/api/finance'

const router = useRouter()

const loading = ref(false)
const list = ref<PayoutOrderVO[]>([])
const total = ref(0)

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  status: '',
  channel: '',
  orderId: '',
  userId: '',
})

async function loadData(): Promise<void> {
  loading.value = true
  try {
    const params: Record<string, unknown> = {
      pageNum: query.pageNum,
      pageSize: query.pageSize,
    }
    if (query.status) params.status = query.status
    if (query.channel) params.channel = query.channel
    if (query.orderId) params.orderId = query.orderId
    if (query.userId) params.userId = query.userId
    const data = await getPayoutPage(params)
    list.value = data?.list ?? []
    total.value = data?.total ?? 0
  } catch {
    // 拦截器已提示
  } finally {
    loading.value = false
  }
}

function handleSearch(): void {
  query.pageNum = 1
  loadData()
}

function handleReset(): void {
  query.status = ''
  query.channel = ''
  query.orderId = ''
  query.userId = ''
  handleSearch()
}

onMounted(loadData)

function goOrderDetail(row: PayoutOrderVO): void {
  if (!row.orderId) return
  router.push(`/order/detail/${row.orderId}`)
}
</script>

<template>
  <div class="page-container">
    <el-card class="filter-card" shadow="never">
      <el-form inline>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable placeholder="全部" style="width: 140px">
            <el-option
              v-for="(item, key) in PAYOUT_STATUS_MAP"
              :key="key"
              :label="item.label"
              :value="key"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="渠道">
          <el-select v-model="query.channel" clearable placeholder="全部" style="width: 140px">
            <el-option
              v-for="(label, key) in PAYOUT_CHANNEL_MAP"
              :key="key"
              :label="label"
              :value="key"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="订单ID">
          <el-input
            v-model.trim="query.orderId"
            placeholder="订单ID"
            clearable
            style="width: 160px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="用户ID">
          <el-input
            v-model.trim="query.userId"
            placeholder="用户ID"
            clearable
            style="width: 160px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="'Search'" @click="handleSearch">查询</el-button>
          <el-button :icon="'RefreshLeft'" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card" shadow="never">
      <div class="table-toolbar">
        <span class="table-title">打款单</span>
        <el-button :icon="'Refresh'" circle @click="loadData" />
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column label="打款单号" min-width="190" show-overflow-tooltip>
          <template #default="{ row }">{{ row.payoutNo || row.id }}</template>
        </el-table-column>
        <el-table-column label="订单ID" width="120">
          <template #default="{ row }">
            <el-link
              v-if="row.orderId"
              type="primary"
              :underline="false"
              @click="goOrderDetail(row)"
            >
              {{ row.orderId }}
            </el-link>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="用户" width="110">
          <template #default="{ row }">{{ row.userId || '-' }}</template>
        </el-table-column>
        <el-table-column label="站点" width="110">
          <template #default="{ row }">{{ row.stationId || '-' }}</template>
        </el-table-column>
        <el-table-column label="渠道" width="110" align="center">
          <template #default="{ row }">{{ payoutChannelText(row.channel) }}</template>
        </el-table-column>
        <el-table-column label="金额(元)" width="110" align="right">
          <template #default="{ row }">
            <span v-if="row.amount" class="amount">¥{{ row.amount }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="120" align="center">
          <template #default="{ row }">
            <el-tag :type="payoutStatusInfo(row.status).type" size="small">
              {{ payoutStatusInfo(row.status).label }}
            </el-tag>
            <div v-if="row.status === 'FAILED' && row.failReason" class="sub-text">
              {{ row.failReason }}
            </div>
          </template>
        </el-table-column>
        <el-table-column label="渠道单号" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ row.channelBillNo || '-' }}</template>
        </el-table-column>
        <el-table-column prop="createTime" label="时间" width="170">
          <template #default="{ row }">{{ row.createTime || '-' }}</template>
        </el-table-column>
      </el-table>

      <div class="pagination-wrap">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50, 100]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="handleSearch"
          @current-change="loadData"
        />
      </div>
    </el-card>
  </div>
</template>

<style scoped lang="scss">
.amount {
  color: #e6a23c;
  font-weight: 600;
}

.sub-text {
  font-size: 12px;
  color: #909399;
  margin-top: 2px;
}
</style>
