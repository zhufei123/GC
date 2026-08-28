<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'

import { auditApply, getApplyPage, type StoreApplyVO } from '@/api/station'

type TagType = 'success' | 'warning' | 'info' | 'danger' | 'primary'

/** 兼容后端字符串 / 数字两种状态枚举 */
const APPLY_STATUS: Record<string, { label: string; type: TagType }> = {
  PENDING: { label: '待审核', type: 'warning' },
  pending: { label: '待审核', type: 'warning' },
  APPROVED: { label: '已通过', type: 'success' },
  approved: { label: '已通过', type: 'success' },
  REJECTED: { label: '已驳回', type: 'danger' },
  rejected: { label: '已驳回', type: 'danger' },
  '0': { label: '待审核', type: 'warning' },
  '1': { label: '已通过', type: 'success' },
  '2': { label: '已驳回', type: 'danger' },
}

function applyStatus(row: StoreApplyVO): string | number | undefined {
  return row.status ?? row.auditStatus
}

function statusInfo(status?: string | number): { label: string; type: TagType } {
  return APPLY_STATUS[String(status ?? '')] ?? { label: String(status ?? '-'), type: 'info' }
}

function isPending(status?: string | number): boolean {
  const value = String(status ?? '').toLowerCase()
  return value === 'pending' || value === '0'
}

const loading = ref(false)
const list = ref<StoreApplyVO[]>([])
const total = ref(0)

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  status: 'PENDING' as string,
})

async function loadData(): Promise<void> {
  loading.value = true
  try {
    const params: Record<string, unknown> = {
      pageNum: query.pageNum,
      pageSize: query.pageSize,
    }
    if (query.status) params.status = query.status.toLowerCase()
    const data = await getApplyPage(params)
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

function storeName(row: StoreApplyVO): string {
  return row.storeName || row.name || '-'
}

function applicant(row: StoreApplyVO): string {
  return row.contactName || row.applicant || '-'
}

function fullAddress(row: StoreApplyVO): string {
  const parts = [row.province, row.city, row.district, row.address, row.detail].filter(Boolean)
  return parts.length ? parts.join('') : '-'
}

function images(row: StoreApplyVO): string[] {
  return [...(row.images ?? []), ...(row.licenseImages ?? [])]
}

/* ---------------- 审核弹窗 ---------------- */
const auditDialogVisible = ref(false)
const submitting = ref(false)
const currentApply = ref<StoreApplyVO | null>(null)
const auditFormRef = ref<FormInstance>()

const auditForm = reactive({
  pass: true,
  remark: '',
})

const auditRules: FormRules = {
  remark: [
    {
      validator: (_rule, value: string, callback) => {
        if (!auditForm.pass && !value) {
          callback(new Error('驳回时必须填写原因'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
}

function openAudit(row: StoreApplyVO, pass: boolean): void {
  currentApply.value = row
  auditForm.pass = pass
  auditForm.remark = ''
  auditDialogVisible.value = true
}

async function submitAudit(): Promise<void> {
  const valid = await auditFormRef.value?.validate().catch(() => false)
  if (!valid || !currentApply.value) return

  submitting.value = true
  try {
    await auditApply(currentApply.value.id, {
      pass: auditForm.pass,
      remark: auditForm.remark || undefined,
    })
    ElMessage.success(auditForm.pass ? '已通过入驻申请' : '已驳回入驻申请')
    auditDialogVisible.value = false
    loadData()
  } catch {
    // 拦截器已提示
  } finally {
    submitting.value = false
  }
}

/* ---------------- 详情抽屉 ---------------- */
const detailVisible = ref(false)
const detailRow = ref<StoreApplyVO | null>(null)

function openDetail(row: StoreApplyVO): void {
  detailRow.value = row
  detailVisible.value = true
}
</script>

<template>
  <div class="page-container">
    <el-card class="filter-card" shadow="never">
      <el-form inline>
        <el-form-item label="审核状态">
          <el-select v-model="query.status" clearable placeholder="全部" style="width: 140px">
            <el-option label="待审核" value="PENDING" />
            <el-option label="已通过" value="APPROVED" />
            <el-option label="已驳回" value="REJECTED" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="'Search'" @click="handleSearch">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card" shadow="never">
      <div class="table-toolbar">
        <span class="table-title">入驻审核</span>
        <el-button :icon="'Refresh'" circle @click="loadData" />
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column label="门店名称" min-width="150">
          <template #default="{ row }">{{ storeName(row) }}</template>
        </el-table-column>
        <el-table-column label="申请人" width="110">
          <template #default="{ row }">{{ applicant(row) }}</template>
        </el-table-column>
        <el-table-column label="电话" width="130">
          <template #default="{ row }">{{ row.phone || row.contactPhone || '-' }}</template>
        </el-table-column>
        <el-table-column label="地址" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">{{ fullAddress(row) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="statusInfo(applyStatus(row)).type" size="small">
              {{ statusInfo(applyStatus(row)).label }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="申请时间" width="170">
          <template #default="{ row }">{{ row.createdAt || row.createTime || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="200" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link @click="openDetail(row)">详情</el-button>
            <template v-if="isPending(applyStatus(row))">
              <el-button
                v-permission="'store:apply:audit'"
                link
                type="success"
                @click="openAudit(row, true)"
              >
                通过
              </el-button>
              <el-button
                v-permission="'store:apply:audit'"
                link
                type="danger"
                @click="openAudit(row, false)"
              >
                驳回
              </el-button>
            </template>
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

    <!-- 审核弹窗 -->
    <el-dialog
      v-model="auditDialogVisible"
      :title="auditForm.pass ? '通过入驻申请' : '驳回入驻申请'"
      width="460px"
      destroy-on-close
    >
      <el-alert
        :type="auditForm.pass ? 'success' : 'error'"
        :closable="false"
        class="audit-alert"
        :title="
          auditForm.pass
            ? `通过后将创建门店「${storeName(currentApply ?? {} as StoreApplyVO)}」,申请人升级为回收站老板`
            : '驳回后申请人可修改资料重新提交'
        "
      />
      <el-form ref="auditFormRef" :model="auditForm" :rules="auditRules" label-width="90px">
        <el-form-item :label="auditForm.pass ? '备注' : '驳回原因'" prop="remark">
          <el-input
            v-model.trim="auditForm.remark"
            type="textarea"
            :rows="3"
            maxlength="200"
            :placeholder="auditForm.pass ? '如:证照齐全(可不填)' : '请填写驳回原因'"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="auditDialogVisible = false">取消</el-button>
        <el-button
          :type="auditForm.pass ? 'success' : 'danger'"
          :loading="submitting"
          @click="submitAudit"
        >
          {{ auditForm.pass ? '确认通过' : '确认驳回' }}
        </el-button>
      </template>
    </el-dialog>

    <!-- 详情抽屉 -->
    <el-drawer v-model="detailVisible" title="入驻申请详情" size="480px">
      <template v-if="detailRow">
        <el-descriptions :column="1" border>
          <el-descriptions-item label="门店名称">{{ storeName(detailRow) }}</el-descriptions-item>
          <el-descriptions-item label="申请人">{{ applicant(detailRow) }}</el-descriptions-item>
          <el-descriptions-item label="电话">{{ detailRow.phone || detailRow.contactPhone || '-' }}</el-descriptions-item>
          <el-descriptions-item label="地址">{{ fullAddress(detailRow) }}</el-descriptions-item>
          <el-descriptions-item label="状态">
            <el-tag :type="statusInfo(applyStatus(detailRow)).type" size="small">
              {{ statusInfo(applyStatus(detailRow)).label }}
            </el-tag>
          </el-descriptions-item>
          <el-descriptions-item label="申请时间">{{ detailRow.createdAt || detailRow.createTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="审核备注">
            {{ detailRow.auditRemark || detailRow.remark || '-' }}
          </el-descriptions-item>
        </el-descriptions>

        <div class="detail-images">
          <div class="detail-images-title">资质/门店照片</div>
          <template v-if="images(detailRow).length">
            <el-image
              v-for="(img, i) in images(detailRow)"
              :key="i"
              :src="img"
              :preview-src-list="images(detailRow)"
              :initial-index="i"
              preview-teleported
              fit="cover"
              class="detail-image"
            />
          </template>
          <el-empty v-else description="暂无照片" :image-size="70" />
        </div>
      </template>
    </el-drawer>
  </div>
</template>

<style scoped lang="scss">
.audit-alert {
  margin-bottom: 16px;
}

.detail-images {
  margin-top: 18px;

  .detail-images-title {
    font-size: 14px;
    font-weight: 600;
    margin-bottom: 10px;
    color: #303133;
  }

  .detail-image {
    width: 96px;
    height: 96px;
    border-radius: 8px;
    margin: 0 8px 8px 0;
  }
}
</style>
