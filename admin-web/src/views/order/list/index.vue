<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'

import {
  cancelOrder,
  getOrderPage,
  ORDER_STATUS_MAP,
  ORDER_TYPE_MAP,
  orderStatusInfo,
  type OrderVO,
} from '@/api/order'

const router = useRouter()

const loading = ref(false)
const list = ref<OrderVO[]>([])
const total = ref(0)

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  orderNo: '',
  status: '',
  dateRange: [] as string[],
})

async function loadData(): Promise<void> {
  loading.value = true
  try {
    const params: Record<string, unknown> = {
      pageNum: query.pageNum,
      pageSize: query.pageSize,
    }
    if (query.orderNo) params.orderNo = query.orderNo
    if (query.status) params.status = query.status
    if (query.dateRange?.length === 2) {
      params.beginDate = query.dateRange[0]
      params.endDate = query.dateRange[1]
    }
    const data = await getOrderPage(params)
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
  query.orderNo = ''
  query.status = ''
  query.dateRange = []
  handleSearch()
}

onMounted(loadData)

function canCancel(row: OrderVO): boolean {
  return !!row.status && row.status !== 'COMPLETED' && row.status !== 'CANCELLED'
}

async function handleCancel(row: OrderVO): Promise<void> {
  let reason = ''
  try {
    const result = await ElMessageBox.prompt(
      `取消订单「${row.orderNo || row.id}」,请填写取消原因`,
      '取消订单',
      {
        type: 'warning',
        inputPlaceholder: '如:用户联系客服要求取消',
        inputValidator: (v: string) => (v && v.trim() ? true : '取消原因必填'),
        confirmButtonText: '确认取消',
        cancelButtonText: '再想想',
      },
    )
    reason = result.value.trim()
  } catch {
    return
  }
  await cancelOrder(row.id, reason)
  ElMessage.success('订单已取消')
  loadData()
}

function goDetail(row: OrderVO): void {
  router.push(`/order/detail/${row.id}`)
}
</script>

<template>
  <div class="page-container">
    <el-card class="filter-card" shadow="never">
      <el-form inline>
        <el-form-item label="订单号">
          <el-input
            v-model.trim="query.orderNo"
            placeholder="订单号"
            clearable
            style="width: 190px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable placeholder="全部" style="width: 130px">
            <el-option
              v-for="(item, key) in ORDER_STATUS_MAP"
              :key="key"
              :label="item.label"
              :value="key"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="下单日期">
          <el-date-picker
            v-model="query.dateRange"
            type="daterange"
            range-separator="至"
            start-placeholder="开始日期"
            end-placeholder="结束日期"
            value-format="YYYY-MM-DD"
            style="width: 260px"
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
        <span class="table-title">回收订单</span>
        <el-button :icon="'Refresh'" circle @click="loadData" />
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column label="订单号" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            <el-link type="primary" :underline="false" @click="goDetail(row)">
              {{ row.orderNo || row.id }}
            </el-link>
          </template>
        </el-table-column>
        <el-table-column label="联系人" width="150">
          <template #default="{ row }">
            <div>{{ row.receiver || '-' }}</div>
            <div v-if="row.phone" class="sub-text">{{ row.phone }}</div>
          </template>
        </el-table-column>
        <el-table-column label="回收站" min-width="140" show-overflow-tooltip>
          <template #default="{ row }">{{ row.stationName || '-' }}</template>
        </el-table-column>
        <el-table-column label="类型" width="100" align="center">
          <template #default="{ row }">
            {{ ORDER_TYPE_MAP[row.type ?? ''] || row.type || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="orderStatusInfo(row.status).type" size="small">
              {{ orderStatusInfo(row.status).label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="金额(元)" width="110" align="right">
          <template #default="{ row }">
            <span v-if="row.actualAmount" class="amount">¥{{ row.actualAmount }}</span>
            <span v-else-if="row.estimateAmount">预估 ¥{{ row.estimateAmount }}</span>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="预约时间" width="170">
          <template #default="{ row }">
            {{ row.appointDate ? `${row.appointDate} ${row.appointPeriod ?? ''}` : '-' }}
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="下单时间" width="170">
          <template #default="{ row }">{{ row.createdAt || row.createTime || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="140" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="goDetail(row)">详情</el-button>
            <el-button
              v-if="canCancel(row)"
              v-permission="'trade:order:cancel'"
              link
              type="danger"
              @click="handleCancel(row)"
            >
              取消
            </el-button>
          </template>
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
}
</style>
