<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'

import {
  createNotice,
  deleteNotice,
  getNoticePage,
  updateNotice,
  type NoticeVO,
} from '@/api/content'

const loading = ref(false)
const list = ref<NoticeVO[]>([])
const total = ref(0)

const query = reactive({
  pageNum: 1,
  pageSize: 10,
})

async function loadData(): Promise<void> {
  loading.value = true
  try {
    const data = await getNoticePage({ ...query })
    list.value = data?.list ?? []
    total.value = data?.total ?? 0
  } catch {
    // 拦截器已提示
  } finally {
    loading.value = false
  }
}

onMounted(loadData)

/* ---------------- 弹窗表单 ---------------- */
const dialogVisible = ref(false)
const submitting = ref(false)
const editId = ref('')
const formRef = ref<FormInstance>()

const form = reactive({
  title: '',
  content: '',
  sort: 0,
  status: 1,
})

const rules: FormRules = {
  title: [{ required: true, message: '请输入公告标题', trigger: 'blur' }],
  content: [{ required: true, message: '请输入公告内容', trigger: 'blur' }],
}

function openDialog(row?: NoticeVO): void {
  editId.value = row?.id ?? ''
  form.title = row?.title ?? ''
  form.content = row?.content ?? ''
  form.sort = row?.sort ?? row?.pinned ?? 0
  form.status = row?.status ?? (row?.publishStatus === 'published' ? 1 : 0)
  dialogVisible.value = true
}

async function submitForm(): Promise<void> {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    if (editId.value) {
      await updateNotice(editId.value, {
        ...form,
        publishStatus: form.status === 1 ? 'published' : 'offline',
      })
      ElMessage.success('修改成功')
    } else {
      await createNotice({
        ...form,
        publishStatus: form.status === 1 ? 'published' : 'offline',
      })
      ElMessage.success('新增成功')
    }
    dialogVisible.value = false
    loadData()
  } catch {
    // 拦截器已提示
  } finally {
    submitting.value = false
  }
}

async function handleDelete(row: NoticeVO): Promise<void> {
  try {
    await ElMessageBox.confirm(`确定删除公告「${row.title}」吗?`, '提示', { type: 'warning' })
  } catch {
    return
  }
  await deleteNotice(row.id)
  ElMessage.success('删除成功')
  loadData()
}
</script>

<template>
  <div class="page-container">
    <el-card class="table-card" shadow="never">
      <div class="table-toolbar">
        <span class="table-title">公告管理</span>
        <div>
          <el-button :icon="'Refresh'" circle @click="loadData" />
          <el-button v-permission="'content:notice:add'" type="primary" :icon="'Plus'" @click="openDialog()">
            新增公告
          </el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="title" label="标题" min-width="180" show-overflow-tooltip />
        <el-table-column label="内容摘要" min-width="260" show-overflow-tooltip>
          <template #default="{ row }">{{ row.content || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 || row.publishStatus === 'published' ? 'success' : 'info'" size="small">
              {{ row.status === 1 || row.publishStatus === 'published' ? '发布' : '下线' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="发布时间" width="170">
          <template #default="{ row }">{{ row.publishedAt || row.publishTime || row.createdAt || row.createTime || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="140" align="center" fixed="right">
          <template #default="{ row }">
            <el-button v-permission="'content:notice:update'" link type="primary" @click="openDialog(row)">
              编辑
            </el-button>
            <el-button v-permission="'content:notice:delete'" link type="danger" @click="handleDelete(row)">
              删除
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
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="editId ? '编辑公告' : '新增公告'"
      width="560px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="公告标题" prop="title">
          <el-input v-model.trim="form.title" maxlength="60" />
        </el-form-item>
        <el-form-item label="公告内容" prop="content">
          <el-input v-model="form.content" type="textarea" :rows="6" maxlength="2000" show-word-limit />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" :max="9999" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">发布</el-radio>
            <el-radio :value="0">下线</el-radio>
          </el-radio-group>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="submitting" @click="submitForm">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>
