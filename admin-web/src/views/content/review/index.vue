<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

import {
  REVIEW_AUDIT_MAP,
  auditReview,
  getReviewPage,
  type OrderReviewAdminVO,
  type ReviewAuditStatus,
} from '@/api/content'

const loading = ref(false)
const list = ref<OrderReviewAdminVO[]>([])
const total = ref(0)

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  auditStatus: 'PENDING' as string,
})

async function loadData(): Promise<void> {
  loading.value = true
  try {
    const params: Record<string, unknown> = {
      pageNum: query.pageNum,
      pageSize: query.pageSize,
    }
    if (query.auditStatus) params.auditStatus = query.auditStatus
    const data = await getReviewPage(params)
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

onMounted(loadData)

async function handleAudit(row: OrderReviewAdminVO, status: ReviewAuditStatus): Promise<void> {
  let remark: string | undefined
  try {
    if (status === 'REJECTED') {
      const { value } = await ElMessageBox.prompt('可填写拒绝原因（选填，仅后台可见）', '拒绝公开', {
        confirmButtonText: '确认拒绝',
        cancelButtonText: '取消',
        inputPlaceholder: '例如：含广告/人身攻击',
        inputType: 'textarea',
      })
      remark = value
    } else {
      await ElMessageBox.confirm('确认通过并公开展示该评价？', '评价审核', {
        type: 'success',
        confirmButtonText: '确认',
        cancelButtonText: '取消',
      })
    }
  } catch {
    return
  }
  await auditReview(row.id, status, remark)
  ElMessage.success('已处理')
  loadData()
}
</script>

<template>
  <div class="page-container">
    <el-card class="filter-card" shadow="never">
      <el-form inline>
        <el-form-item label="审核状态">
          <el-select v-model="query.auditStatus" clearable placeholder="全部" style="width: 140px">
            <el-option
              v-for="(item, key) in REVIEW_AUDIT_MAP"
              :key="key"
              :label="item.label"
              :value="key"
            />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" @click="handleSearch">查询</el-button>
          <el-button
            @click="
              query.auditStatus = '';
              handleSearch()
            "
          >
            重置
          </el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card" shadow="never">
      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column label="订单ID" prop="orderId" min-width="160" />
        <el-table-column label="用户ID" prop="userId" width="140" />
        <el-table-column label="门店ID" prop="stationId" width="140" />
        <el-table-column label="评分" prop="rating" width="80" align="center" />
        <el-table-column label="评论" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">{{ row.comment || '（仅评分）' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="110" align="center">
          <template #default="{ row }">
            <el-tag :type="REVIEW_AUDIT_MAP[row.auditStatus || '']?.type || 'info'" size="small">
              {{ REVIEW_AUDIT_MAP[row.auditStatus || '']?.label || row.auditStatus || '-' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="时间" prop="createTime" width="180" />
        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <template v-if="row.auditStatus === 'PENDING'">
              <el-button type="success" link @click="handleAudit(row, 'APPROVED')">通过</el-button>
              <el-button type="danger" link @click="handleAudit(row, 'REJECTED')">拒绝</el-button>
            </template>
            <span v-else class="muted">已处理</span>
          </template>
        </el-table-column>
      </el-table>
      <div class="pager">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :total="total"
          layout="total, prev, pager, next"
          @current-change="loadData"
        />
      </div>
    </el-card>
  </div>
</template>

<style scoped>
.muted {
  color: #c0c4cc;
  font-size: 13px;
}
.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}
</style>
