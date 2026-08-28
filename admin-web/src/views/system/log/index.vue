<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'

import { getOplogPage, type OpLogVO } from '@/api/system'

const loading = ref(false)
const list = ref<OpLogVO[]>([])
const total = ref(0)

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  dateRange: [] as string[],
})

async function loadData(): Promise<void> {
  loading.value = true
  try {
    const params: Record<string, unknown> = {
      pageNum: query.pageNum,
      pageSize: query.pageSize,
    }
    if (query.keyword) params.keyword = query.keyword
    if (query.dateRange?.length === 2) {
      params.beginDate = query.dateRange[0]
      params.endDate = query.dateRange[1]
    }
    const data = await getOplogPage(params)
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

/* ---------------- 详情弹窗 ---------------- */
const detailVisible = ref(false)
const detailRow = ref<OpLogVO | null>(null)

function openDetail(row: OpLogVO): void {
  detailRow.value = row
  detailVisible.value = true
}

function prettyJson(text?: string): string {
  if (!text) return '-'
  try {
    return JSON.stringify(JSON.parse(text), null, 2)
  } catch {
    return text
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
            placeholder="操作人 / 操作内容"
            clearable
            style="width: 200px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="操作日期">
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
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card" shadow="never">
      <div class="table-toolbar">
        <span class="table-title">操作日志</span>
        <el-button :icon="'Refresh'" circle @click="loadData" />
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column label="操作人" width="120">
          <template #default="{ row }">{{ row.adminName || row.operator || '-' }}</template>
        </el-table-column>
        <el-table-column label="模块 / 操作" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">
            {{ [row.module, row.title].filter(Boolean).join(' / ') || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="请求" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">
            <el-tag v-if="row.method" size="small" type="info" effect="plain" class="method-tag">
              {{ row.method }}
            </el-tag>
            {{ row.uri || '-' }}
          </template>
        </el-table-column>
        <el-table-column label="IP" width="130">
          <template #default="{ row }">{{ row.ip || '-' }}</template>
        </el-table-column>
        <el-table-column label="耗时" width="90" align="right">
          <template #default="{ row }">
            {{ row.costMs !== undefined && row.costMs !== null ? `${row.costMs}ms` : '-' }}
          </template>
        </el-table-column>
        <el-table-column label="结果" width="80" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 || row.status === undefined ? 'success' : 'danger'" size="small">
              {{ row.status === 0 || row.status === undefined ? '成功' : '失败' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="操作时间" width="170">
          <template #default="{ row }">{{ row.createdAt || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="80" align="center" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
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

    <el-dialog v-model="detailVisible" title="日志详情" width="640px">
      <template v-if="detailRow">
        <el-descriptions :column="2" border size="small">
          <el-descriptions-item label="操作人">
            {{ detailRow.adminName || detailRow.operator || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="操作时间">{{ detailRow.createdAt || '-' }}</el-descriptions-item>
          <el-descriptions-item label="模块">{{ detailRow.module || '-' }}</el-descriptions-item>
          <el-descriptions-item label="操作">{{ detailRow.title || '-' }}</el-descriptions-item>
          <el-descriptions-item label="请求" :span="2">
            {{ detailRow.method || '' }} {{ detailRow.uri || '-' }}
          </el-descriptions-item>
          <el-descriptions-item label="IP">{{ detailRow.ip || '-' }}</el-descriptions-item>
          <el-descriptions-item label="耗时">
            {{ detailRow.costMs !== undefined && detailRow.costMs !== null ? `${detailRow.costMs}ms` : '-' }}
          </el-descriptions-item>
        </el-descriptions>
        <div class="json-block">
          <div class="json-title">请求参数</div>
          <pre>{{ prettyJson(detailRow.params) }}</pre>
        </div>
        <div class="json-block">
          <div class="json-title">返回结果 / 错误</div>
          <pre>{{ prettyJson(detailRow.result || detailRow.errorMsg) }}</pre>
        </div>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped lang="scss">
.method-tag {
  margin-right: 6px;
}

.json-block {
  margin-top: 14px;

  .json-title {
    font-size: 13px;
    font-weight: 600;
    color: #303133;
    margin-bottom: 6px;
  }

  pre {
    margin: 0;
    padding: 10px 12px;
    background: #f7f9f8;
    border: 1px solid #e8ece9;
    border-radius: 6px;
    font-size: 12px;
    max-height: 220px;
    overflow: auto;
    white-space: pre-wrap;
    word-break: break-all;
  }
}
</style>
