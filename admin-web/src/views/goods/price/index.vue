<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'

import {
  getSkuPage,
  getSkuPriceLog,
  updateSkuPrice,
  type PriceLogVO,
  type SkuVO,
} from '@/api/goods'

const loading = ref(false)
const list = ref<SkuVO[]>([])
const total = ref(0)

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
})

async function loadData(): Promise<void> {
  loading.value = true
  try {
    const params: Record<string, unknown> = {
      pageNum: query.pageNum,
      pageSize: query.pageSize,
    }
    if (query.keyword) params.keyword = query.keyword
    const data = await getSkuPage(params)
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

/* ---------------- 调价弹窗 ---------------- */
const priceDialogVisible = ref(false)
const submitting = ref(false)
const currentSku = ref<SkuVO | null>(null)
const priceFormRef = ref<FormInstance>()

const priceForm = reactive({
  price: '',
  effectiveAt: '',
  reason: '',
})

const priceRules: FormRules = {
  price: [
    { required: true, message: '请输入新价格', trigger: 'blur' },
    { pattern: /^\d+(\.\d{1,2})?$/, message: '价格格式如 0.92', trigger: 'blur' },
  ],
}

function openPriceDialog(row: SkuVO): void {
  currentSku.value = row
  priceForm.price = row.price ?? ''
  priceForm.effectiveAt = ''
  priceForm.reason = ''
  priceDialogVisible.value = true
}

async function submitPrice(): Promise<void> {
  const valid = await priceFormRef.value?.validate().catch(() => false)
  if (!valid || !currentSku.value) return

  submitting.value = true
  try {
    await updateSkuPrice(currentSku.value.id, {
      price: priceForm.price,
      effectiveAt: priceForm.effectiveAt || undefined,
      reason: priceForm.reason || undefined,
    })
    ElMessage.success('调价成功')
    priceDialogVisible.value = false
    loadData()
  } catch {
    // 拦截器已提示
  } finally {
    submitting.value = false
  }
}

/* ---------------- 调价日志抽屉 ---------------- */
const drawerVisible = ref(false)
const logLoading = ref(false)
const logs = ref<PriceLogVO[]>([])
const logSku = ref<SkuVO | null>(null)

async function openLogDrawer(row: SkuVO): Promise<void> {
  logSku.value = row
  drawerVisible.value = true
  logLoading.value = true
  logs.value = []
  try {
    const data = await getSkuPriceLog(row.id, { pageNum: 1, pageSize: 100 })
    logs.value = Array.isArray(data) ? data : (data?.list ?? [])
  } catch {
    // 拦截器已提示
  } finally {
    logLoading.value = false
  }
}
</script>

<template>
  <div class="page-container">
    <el-card class="filter-card" shadow="never">
      <el-form inline>
        <el-form-item label="关键词">
          <el-input
            v-model.trim="query.keyword"
            placeholder="品类名称"
            clearable
            style="width: 200px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="'Search'" @click="handleSearch">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card" shadow="never">
      <div class="table-toolbar">
        <span class="table-title">价格管理</span>
        <el-tag type="info" effect="plain">城市维度 P0 默认「全国」</el-tag>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="name" label="品类名称" min-width="180" />
        <el-table-column label="所属分类" min-width="120">
          <template #default="{ row }">{{ row.categoryName || row.categoryId || '-' }}</template>
        </el-table-column>
        <el-table-column prop="unit" label="单位" width="80" align="center" />
        <el-table-column label="当前价" width="130" align="right">
          <template #default="{ row }">
            <span v-if="row.price" class="price-text">¥{{ row.price }} / {{ row.unit }}</span>
            <el-tag v-else size="small" type="info">暂无报价</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '上架' : '下架' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180" align="center" fixed="right">
          <template #default="{ row }">
            <el-button
              v-permission="'recycle:sku:price'"
              link
              type="primary"
              :icon="'EditPen'"
              @click="openPriceDialog(row)"
            >
              调价
            </el-button>
            <el-button link :icon="'Clock'" @click="openLogDrawer(row)">日志</el-button>
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

    <!-- 调价弹窗 -->
    <el-dialog v-model="priceDialogVisible" title="价格调整" width="460px" destroy-on-close>
      <el-descriptions v-if="currentSku" :column="2" border size="small" class="sku-desc">
        <el-descriptions-item label="品类">{{ currentSku.name }}</el-descriptions-item>
        <el-descriptions-item label="当前价">
          {{ currentSku.price ? `¥${currentSku.price} / ${currentSku.unit}` : '暂无报价' }}
        </el-descriptions-item>
      </el-descriptions>
      <el-form ref="priceFormRef" :model="priceForm" :rules="priceRules" label-width="90px">
        <el-form-item label="新价格" prop="price">
          <el-input v-model.trim="priceForm.price" placeholder="如 0.92" style="width: 200px">
            <template #prefix>¥</template>
          </el-input>
        </el-form-item>
        <el-form-item label="生效时间">
          <el-date-picker
            v-model="priceForm.effectiveAt"
            type="datetime"
            placeholder="留空立即生效"
            value-format="YYYY-MM-DD HH:mm:ss"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="调价备注">
          <el-input
            v-model.trim="priceForm.reason"
            type="textarea"
            :rows="2"
            maxlength="100"
            placeholder="如:纸价上调"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="priceDialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitPrice">确认调价</el-button>
      </template>
    </el-dialog>

    <!-- 调价日志抽屉 -->
    <el-drawer v-model="drawerVisible" :title="`调价日志 - ${logSku?.name ?? ''}`" size="480px">
      <el-table v-loading="logLoading" :data="logs" border stripe size="small">
        <el-table-column label="原价" width="90" align="right">
          <template #default="{ row }">{{ row.oldPrice ? `¥${row.oldPrice}` : '-' }}</template>
        </el-table-column>
        <el-table-column label="新价" width="90" align="right">
          <template #default="{ row }">
            <span class="price-text">¥{{ row.newPrice || row.price || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column label="备注" min-width="110">
          <template #default="{ row }">{{ row.reason || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作人 / 时间" min-width="150">
          <template #default="{ row }">
            <div>{{ row.operatorName || row.operator || '-' }}</div>
            <div class="log-time">{{ row.effectiveAt || row.createdAt || '-' }}</div>
          </template>
        </el-table-column>
        <template #empty>
          <el-empty description="暂无调价记录" :image-size="70" />
        </template>
      </el-table>
    </el-drawer>
  </div>
</template>

<style scoped lang="scss">
.price-text {
  color: #e6a23c;
  font-weight: 600;
}

.sku-desc {
  margin-bottom: 16px;
}

.log-time {
  font-size: 12px;
  color: #909399;
}
</style>
