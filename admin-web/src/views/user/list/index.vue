<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'

import { getUserPage, updateUserStatus, type MemberUserVO } from '@/api/user'

const loading = ref(false)
const list = ref<MemberUserVO[]>([])
const total = ref(0)

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  role: '',
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
    if (query.role) params.role = query.role
    if (query.status !== '') params.status = query.status
    const data = await getUserPage(params)
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

async function handleStatusChange(row: MemberUserVO): Promise<void> {
  const target = row.status === 1 ? 0 : 1
  const action = target === 1 ? '启用' : '禁用'
  try {
    await ElMessageBox.confirm(
      `确定${action}用户「${row.nickname || row.id}」吗?`,
      '提示',
      { type: 'warning' },
    )
  } catch {
    return
  }
  await updateUserStatus(row.id, target)
  row.status = target
  ElMessage.success(`${action}成功`)
}
</script>

<template>
  <div class="page-container">
    <el-card class="filter-card" shadow="never">
      <el-form inline>
        <el-form-item label="关键词">
          <el-input
            v-model.trim="query.keyword"
            placeholder="昵称 / 手机号"
            clearable
            style="width: 200px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="角色">
          <el-select v-model="query.role" clearable placeholder="全部" style="width: 150px">
            <el-option label="普通用户" value="customer" />
            <el-option label="回收站老板" value="recycler" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable placeholder="全部" style="width: 120px">
            <el-option label="正常" :value="1" />
            <el-option label="禁用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="'Search'" @click="handleSearch">查询</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card" shadow="never">
      <div class="table-toolbar">
        <span class="table-title">用户列表</span>
        <el-button :icon="'Refresh'" circle @click="loadData" />
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="id" label="用户ID" width="150" show-overflow-tooltip />
        <el-table-column label="用户" min-width="160">
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar :size="32" :src="row.avatar || undefined">
                {{ (row.nickname || '用').slice(0, 1) }}
              </el-avatar>
              <span>{{ row.nickname || '-' }}</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="手机号" width="140">
          <template #default="{ row }">{{ row.phone || '-' }}</template>
        </el-table-column>
        <el-table-column label="角色" width="120" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.role === 'recycler' ? 'warning' : 'primary'">
              {{ row.role === 'recycler' ? '回收站老板' : '普通用户' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '正常' : '禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="注册时间" width="170">
          <template #default="{ row }">{{ row.createdAt || row.createTime || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="110" align="center" fixed="right">
          <template #default="{ row }">
            <el-button
              v-permission="'member:user:update'"
              link
              :type="row.status === 1 ? 'danger' : 'success'"
              @click="handleStatusChange(row)"
            >
              {{ row.status === 1 ? '禁用' : '启用' }}
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
.user-cell {
  display: flex;
  align-items: center;
  gap: 8px;
}
</style>
