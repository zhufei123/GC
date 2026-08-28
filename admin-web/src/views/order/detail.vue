<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'

import {
  cancelOrder,
  getOrderDetail,
  ORDER_TYPE_MAP,
  orderStatusInfo,
  type OrderItemVO,
  type OrderTimelineVO,
  type OrderVO,
} from '@/api/order'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const detail = ref<OrderVO | null>(null)

const orderId = computed(() => String(route.params.id ?? ''))

async function loadData(): Promise<void> {
  if (!orderId.value) return
  loading.value = true
  try {
    detail.value = await getOrderDetail(orderId.value)
  } catch {
    // 拦截器已提示
  } finally {
    loading.value = false
  }
}

onMounted(loadData)

const estimateItems = computed<OrderItemVO[]>(() => detail.value?.estimateItems ?? [])
const actualItems = computed<OrderItemVO[]>(() => detail.value?.actualItems ?? [])
const allImages = computed<string[]>(() => [
  ...(detail.value?.images ?? []),
  ...(detail.value?.weighImages ?? []),
])

const timeline = computed<OrderTimelineVO[]>(() => detail.value?.timeline ?? [])

const addressText = computed(() => {
  const d = detail.value
  if (!d) return '-'
  if (typeof d.addressSnapshot === 'string' && d.addressSnapshot) return d.addressSnapshot
  if (d.addressSnapshot && typeof d.addressSnapshot === 'object') {
    const snap = d.addressSnapshot as Record<string, unknown>
    const parts = ['province', 'city', 'district', 'street', 'detail']
      .map((k) => snap[k])
      .filter(Boolean)
    const receiver = [snap.receiver, snap.phone].filter(Boolean).join(' ')
    const addr = parts.join('')
    return [receiver, addr].filter(Boolean).join(' | ') || '-'
  }
  return d.address || '-'
})

const canCancel = computed(() => {
  const s = detail.value?.status
  return !!s && s !== 'COMPLETED' && s !== 'CANCELLED'
})

/** 时间线：当前状态 primary，已发生 success，未到达默认灰 */
function timelineType(item: OrderTimelineVO): 'primary' | 'success' | undefined {
  const current = detail.value?.status
  if (!item.status || !current) return undefined
  if (item.status === current) return 'primary'
  if (item.time || item.createdAt) return 'success'
  return undefined
}

async function handleCancel(): Promise<void> {
  if (!detail.value) return
  let reason = ''
  try {
    const result = await ElMessageBox.prompt('请填写取消原因', '取消订单', {
      type: 'warning',
      inputValidator: (v: string) => (v && v.trim() ? true : '取消原因必填'),
      confirmButtonText: '确认取消',
      cancelButtonText: '再想想',
    })
    reason = result.value.trim()
  } catch {
    return
  }
  await cancelOrder(detail.value.id, reason)
  ElMessage.success('订单已取消')
  loadData()
}
</script>

<template>
  <div v-loading="loading" class="page-container order-detail">
    <el-card shadow="never" class="head-card">
      <div class="head-row">
        <div class="head-left">
          <el-button :icon="'ArrowLeft'" circle @click="router.back()" />
          <div>
            <div class="order-no">订单号:{{ detail?.orderNo || detail?.id || '-' }}</div>
            <div class="order-sub">
              下单时间:{{ detail?.createdAt || detail?.createTime || '-' }}
              <span v-if="detail?.type" class="order-type">
                {{ ORDER_TYPE_MAP[detail.type] || detail.type }}
              </span>
            </div>
          </div>
        </div>
        <div class="head-right">
          <el-tag :type="orderStatusInfo(detail?.status).type" size="large">
            {{ orderStatusInfo(detail?.status).label }}
          </el-tag>
          <el-button
            v-if="canCancel"
            v-permission="'trade:order:cancel'"
            type="danger"
            plain
            @click="handleCancel"
          >
            取消订单
          </el-button>
        </div>
      </div>
    </el-card>

    <el-row :gutter="16">
      <el-col :span="16">
        <el-card shadow="never" header="基本信息" class="mb16">
          <el-descriptions :column="2" border>
            <el-descriptions-item label="联系人">
              {{ detail?.receiver || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="联系电话">
              {{ detail?.phone || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="回收站">
              {{ detail?.stationName || '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="预约时间">
              {{ detail?.appointDate ? `${detail.appointDate} ${detail.appointPeriod ?? ''}` : '-' }}
            </el-descriptions-item>
            <el-descriptions-item label="地址" :span="2">{{ addressText }}</el-descriptions-item>
            <el-descriptions-item label="备注" :span="2">
              {{ detail?.remark || '-' }}
            </el-descriptions-item>
            <el-descriptions-item v-if="detail?.cancelReason" label="取消原因" :span="2">
              {{ detail.cancelReason }}
            </el-descriptions-item>
          </el-descriptions>
        </el-card>

        <el-card shadow="never" header="预估明细" class="mb16">
          <el-table :data="estimateItems" border size="small">
            <el-table-column label="品类" min-width="150">
              <template #default="{ row }">{{ row.skuName || row.skuId || '-' }}</template>
            </el-table-column>
            <el-table-column label="预估重量(kg)" width="130" align="right">
              <template #default="{ row }">{{ row.estimateWeight || row.weight || '-' }}</template>
            </el-table-column>
            <el-table-column label="参考价(元)" width="120" align="right">
              <template #default="{ row }">{{ row.price ? `¥${row.price}` : '-' }}</template>
            </el-table-column>
            <template #empty>
              <el-empty description="暂无预估明细" :image-size="60" />
            </template>
          </el-table>
          <div v-if="detail?.estimateAmount" class="amount-row">
            预估金额:<span class="amount">¥{{ detail.estimateAmount }}</span>
          </div>
        </el-card>

        <el-card shadow="never" header="实收明细(称重)" class="mb16">
          <el-table :data="actualItems" border size="small">
            <el-table-column label="品类" min-width="150">
              <template #default="{ row }">{{ row.skuName || row.skuId || '-' }}</template>
            </el-table-column>
            <el-table-column label="实际重量(kg)" width="130" align="right">
              <template #default="{ row }">{{ row.weight || '-' }}</template>
            </el-table-column>
            <el-table-column label="成交单价(元)" width="130" align="right">
              <template #default="{ row }">{{ row.price ? `¥${row.price}` : '-' }}</template>
            </el-table-column>
            <el-table-column label="小计(元)" width="110" align="right">
              <template #default="{ row }">{{ row.amount ? `¥${row.amount}` : '-' }}</template>
            </el-table-column>
            <template #empty>
              <el-empty description="尚未称重" :image-size="60" />
            </template>
          </el-table>
          <div v-if="detail?.actualAmount" class="amount-row">
            实收总额:
            <span class="amount total">¥{{ detail.actualAmount }}</span>
          </div>
        </el-card>

        <el-card shadow="never" header="现场照片">
          <template v-if="allImages.length">
            <el-image
              v-for="(img, i) in allImages"
              :key="i"
              :src="img"
              :preview-src-list="allImages"
              :initial-index="i"
              preview-teleported
              fit="cover"
              class="photo"
            />
          </template>
          <el-empty v-else description="暂无照片" :image-size="70" />
        </el-card>
      </el-col>

      <el-col :span="8">
        <el-card shadow="never" header="状态时间线">
          <el-timeline v-if="timeline.length">
            <el-timeline-item
              v-for="(item, i) in timeline"
              :key="i"
              :timestamp="item.time || item.createdAt || (item.status === detail?.status ? '进行中' : '未到达')"
              :type="timelineType(item)"
              :hollow="!(item.time || item.createdAt) && item.status !== detail?.status"
            >
              <div class="timeline-title">
                {{ item.title || item.label || orderStatusInfo(item.status).label }}
              </div>
              <div v-if="item.remark" class="timeline-remark">{{ item.remark }}</div>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else description="暂无流转记录" :image-size="70" />
        </el-card>
      </el-col>
    </el-row>
  </div>
</template>

<style scoped lang="scss">
.order-detail {
  .mb16 {
    margin-bottom: 16px;
  }

  .head-card {
    margin-bottom: 16px;
  }

  .head-row {
    display: flex;
    align-items: center;
    justify-content: space-between;

    .head-left {
      display: flex;
      align-items: center;
      gap: 14px;

      .order-no {
        font-size: 16px;
        font-weight: 600;
        color: #1f2d27;
      }

      .order-sub {
        font-size: 12px;
        color: #8a9a91;
        margin-top: 4px;

        .order-type {
          margin-left: 10px;
          color: #07c160;
        }
      }
    }

    .head-right {
      display: flex;
      align-items: center;
      gap: 12px;
    }
  }

  .amount-row {
    text-align: right;
    margin-top: 12px;
    font-size: 14px;
    color: #606266;

    .amount {
      color: #e6a23c;
      font-weight: 700;

      &.total {
        font-size: 18px;
      }
    }
  }

  .photo {
    width: 96px;
    height: 96px;
    border-radius: 8px;
    margin: 0 8px 8px 0;
  }

  .timeline-title {
    font-weight: 600;
    color: #303133;
  }

  .timeline-remark {
    font-size: 12px;
    color: #909399;
    margin-top: 4px;
  }
}
</style>
