<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'

import {
  createBanner,
  deleteBanner,
  getBannerPage,
  updateBanner,
  type BannerVO,
} from '@/api/content'

const loading = ref(false)
const list = ref<BannerVO[]>([])
const total = ref(0)

const query = reactive({
  pageNum: 1,
  pageSize: 10,
})

async function loadData(): Promise<void> {
  loading.value = true
  try {
    const data = await getBannerPage({ ...query })
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
  image: '',
  linkUrl: '',
  sort: 0,
  status: 1,
})

const rules: FormRules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  image: [{ required: true, message: '请输入图片 URL', trigger: 'blur' }],
}

function openDialog(row?: BannerVO): void {
  editId.value = row?.id ?? ''
  form.title = row?.title ?? ''
  form.image = row?.image ?? ''
  form.linkUrl = row?.linkUrl ?? ''
  form.sort = row?.sort ?? 0
  form.status = row?.status ?? 1
  dialogVisible.value = true
}

async function submitForm(): Promise<void> {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    if (editId.value) {
      await updateBanner(editId.value, { ...form })
      ElMessage.success('修改成功')
    } else {
      await createBanner({ ...form })
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

async function handleDelete(row: BannerVO): Promise<void> {
  try {
    await ElMessageBox.confirm(`确定删除轮播图「${row.title}」吗?`, '提示', { type: 'warning' })
  } catch {
    return
  }
  await deleteBanner(row.id)
  ElMessage.success('删除成功')
  loadData()
}
</script>

<template>
  <div class="page-container">
    <el-card class="table-card" shadow="never">
      <div class="table-toolbar">
        <span class="table-title">首页轮播图</span>
        <div>
          <el-button :icon="'Refresh'" circle @click="loadData" />
          <el-button v-permission="'content:banner:add'" type="primary" :icon="'Plus'" @click="openDialog()">
            新增轮播图
          </el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column label="图片" width="140" align="center">
          <template #default="{ row }">
            <el-image
              v-if="row.image"
              :src="row.image"
              :preview-src-list="[row.image]"
              preview-teleported
              fit="cover"
              style="width: 110px; height: 50px; border-radius: 6px"
            />
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="标题" min-width="160" show-overflow-tooltip />
        <el-table-column label="跳转链接" min-width="200" show-overflow-tooltip>
          <template #default="{ row }">{{ row.linkUrl || '-' }}</template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="80" align="center" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '展示' : '隐藏' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="140" align="center" fixed="right">
          <template #default="{ row }">
            <el-button v-permission="'content:banner:update'" link type="primary" @click="openDialog(row)">
              编辑
            </el-button>
            <el-button v-permission="'content:banner:delete'" link type="danger" @click="handleDelete(row)">
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
      :title="editId ? '编辑轮播图' : '新增轮播图'"
      width="500px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="标题" prop="title">
          <el-input v-model.trim="form.title" maxlength="40" />
        </el-form-item>
        <el-form-item label="图片 URL" prop="image">
          <el-input v-model.trim="form.image" placeholder="https://..." />
        </el-form-item>
        <el-form-item label="跳转链接">
          <el-input v-model.trim="form.linkUrl" placeholder="可选,点击轮播图跳转地址" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" :max="9999" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">展示</el-radio>
            <el-radio :value="0">隐藏</el-radio>
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
