<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

import {
  getStorePage,
  updateStore,
  updateStoreStatus,
  type StoreVO,
} from '@/api/station'

const loading = ref(false)
const list = ref<StoreVO[]>([])
const total = ref(0)

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  status: '' as '' | number,
})

async function loadData(): Promise<void> {
  loading.value = true
  try {
    const params: Record<string, unknown> = {
      pageNum: query.pageNum,
      pageSize: query.pageSize,
    }
    if (query.keyword) params.keyword = query.keyword
    if (query.status !== '') params.status = query.status
    const data = await getStorePage(params)
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

function fullAddress(row: StoreVO): string {
  const parts = [row.province, row.city, row.district, row.address, row.detail].filter(Boolean)
  return parts.length ? parts.join('') : '-'
}

function isStationOpen(status: StoreVO['businessStatus']): boolean {
  return status === 'OPEN' || status === 1 || status === '1'
}

async function handleStatusChange(row: StoreVO): Promise<void> {
  const target = row.status === 1 ? 0 : 1
  const action = target === 1 ? '启用' : '停用'
  try {
    await ElMessageBox.confirm(`确定${action}门店「${row.name}」吗?`, '提示', { type: 'warning' })
  } catch {
    return
  }
  await updateStoreStatus(row.id, target)
  row.status = target
  ElMessage.success(`${action}成功`)
}

/* ---------------- 编辑弹窗 ---------------- */
const dialogVisible = ref(false)
const submitting = ref(false)
const editId = ref('')

const form = reactive({
  name: '',
  contactName: '',
  phone: '',
  address: '',
})

function openDialog(row: StoreVO): void {
  editId.value = row.id
  form.name = row.name ?? ''
  form.contactName = row.contactName ?? row.bossName ?? ''
  form.phone = row.phone ?? ''
  form.address = row.address ?? ''
  dialogVisible.value = true
}

async function submitForm(): Promise<void> {
  submitting.value = true
  try {
    await updateStore(editId.value, { ...form })
    ElMessage.success('修改成功')
    dialogVisible.value = false
    loadData()
  } catch {
    // 拦截器已提示
  } finally {
    submitting.value = false
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
            placeholder="门店名 / 联系电话"
            clearable
            style="width: 200px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable placeholder="全部" style="width: 120px">
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="'Search'" @click="handleSearch">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card" shadow="never">
      <div class="table-toolbar">
        <span class="table-title">回收站门店</span>
        <el-button :icon="'Refresh'" circle @click="loadData" />
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="name" label="门店名称" min-width="160" />
        <el-table-column label="联系人" width="110">
          <template #default="{ row }">{{ row.contactName || row.bossName || '-' }}</template>
        </el-table-column>
        <el-table-column label="电话" width="130">
          <template #default="{ row }">{{ row.phone || '-' }}</template>
        </el-table-column>
        <el-table-column label="地址" min-width="220" show-overflow-tooltip>
          <template #default="{ row }">{{ fullAddress(row) }}</template>
        </el-table-column>
        <el-table-column label="营业状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag
              v-if="row.businessStatus !== undefined && row.businessStatus !== null && row.businessStatus !== ''"
              size="small"
              :type="isStationOpen(row.businessStatus) ? 'success' : 'info'"
            >
              {{ isStationOpen(row.businessStatus) ? '营业中' : '休息中' }}
            </el-tag>
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="入驻时间" width="170">
          <template #default="{ row }">{{ row.createdAt || row.createTime || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="150" align="center" fixed="right">
          <template #default="{ row }">
            <el-button v-permission="'store:store:update'" link type="primary" @click="openDialog(row)">
              编辑
            </el-button>
            <el-button
              v-permission="'store:store:update'"
              link
              :type="row.status === 1 ? 'danger' : 'success'"
              @click="handleStatusChange(row)"
            >
              {{ row.status === 1 ? '停用' : '启用' }}
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

    <el-dialog v-model="dialogVisible" title="编辑门店" width="480px" destroy-on-close>
      <el-form :model="form" label-width="80px">
        <el-form-item label="门店名称">
          <el-input v-model.trim="form.name" maxlength="40" />
        </el-form-item>
        <el-form-item label="联系人">
          <el-input v-model.trim="form.contactName" maxlength="20" />
        </el-form-item>
        <el-form-item label="电话">
          <el-input v-model.trim="form.phone" maxlength="20" />
        </el-form-item>
        <el-form-item label="地址">
          <el-input v-model.trim="form.address" type="textarea" :rows="2" maxlength="120" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>
