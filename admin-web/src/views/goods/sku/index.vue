<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'

import {
  createSku,
  deleteSku,
  getCategoryTree,
  getSkuPage,
  updateSku,
  updateSkuStatus,
  type CategoryVO,
  type SkuVO,
} from '@/api/goods'

const loading = ref(false)
const list = ref<SkuVO[]>([])
const total = ref(0)
const categoryTree = ref<CategoryVO[]>([])

const query = reactive({
  pageNum: 1,
  pageSize: 10,
  keyword: '',
  categoryId: '',
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
    if (query.categoryId) params.categoryId = query.categoryId
    if (query.status !== '') params.status = query.status
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

function handleReset(): void {
  query.keyword = ''
  query.categoryId = ''
  query.status = ''
  handleSearch()
}

onMounted(async () => {
  loadData()
  try {
    categoryTree.value = (await getCategoryTree()) ?? []
  } catch {
    // 忽略,仅影响筛选下拉
  }
})

/* ---------------- 弹窗表单 ---------------- */
const dialogVisible = ref(false)
const submitting = ref(false)
const editId = ref('')
const formRef = ref<FormInstance>()

const form = reactive({
  categoryId: '',
  name: '',
  image: '',
  unit: 'kg',
  price: '',
  sort: 0,
  status: 1,
})

const rules: FormRules = {
  categoryId: [{ required: true, message: '请选择分类', trigger: 'change' }],
  name: [{ required: true, message: '请输入品类名称', trigger: 'blur' }],
  unit: [{ required: true, message: '请输入计量单位', trigger: 'blur' }],
  price: [
    {
      pattern: /^\d+(\.\d{1,2})?$/,
      message: '价格格式如 0.85',
      trigger: 'blur',
    },
  ],
}

function openDialog(row?: SkuVO): void {
  editId.value = row?.id ?? ''
  form.categoryId = row?.categoryId ?? ''
  form.name = row?.name ?? ''
  form.image = row?.image ?? ''
  form.unit = row?.unit ?? 'kg'
  form.price = row?.price ?? ''
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
      await updateSku(editId.value, { ...form })
      ElMessage.success('修改成功')
    } else {
      await createSku({ ...form })
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

async function handleDelete(row: SkuVO): Promise<void> {
  try {
    await ElMessageBox.confirm(`确定删除品类「${row.name}」吗?`, '提示', { type: 'warning' })
  } catch {
    return
  }
  await deleteSku(row.id)
  ElMessage.success('删除成功')
  loadData()
}

async function handleStatusChange(row: SkuVO): Promise<void> {
  const target = row.status === 1 ? 0 : 1
  try {
    await updateSkuStatus(row.id, target)
    row.status = target
    ElMessage.success(target === 1 ? '已上架' : '已下架')
  } catch {
    // 保持原状态
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
            style="width: 180px"
            @keyup.enter="handleSearch"
          />
        </el-form-item>
        <el-form-item label="分类">
          <el-tree-select
            v-model="query.categoryId"
            :data="categoryTree"
            :props="{ label: 'name', value: 'id', children: 'children' }"
            node-key="id"
            check-strictly
            clearable
            placeholder="全部分类"
            style="width: 180px"
          />
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="query.status" clearable placeholder="全部" style="width: 120px">
            <el-option label="上架" :value="1" />
            <el-option label="下架" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="'Search'" @click="handleSearch">查询</el-button>
          <el-button :icon="'RefreshLeft'" @click="handleReset">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card class="table-card" shadow="never">
      <div class="table-toolbar">
        <span class="table-title">回收品类(SKU)</span>
        <el-button v-permission="'recycle:sku:add'" type="primary" :icon="'Plus'" @click="openDialog()">
          新增品类
        </el-button>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="name" label="品类名称" min-width="170" />
        <el-table-column label="所属分类" min-width="120">
          <template #default="{ row }">{{ row.categoryName || row.categoryId || '-' }}</template>
        </el-table-column>
        <el-table-column label="图片" width="90" align="center">
          <template #default="{ row }">
            <el-image
              v-if="row.image"
              :src="row.image"
              :preview-src-list="[row.image]"
              preview-teleported
              fit="cover"
              style="width: 40px; height: 40px; border-radius: 6px"
            />
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column prop="unit" label="单位" width="80" align="center" />
        <el-table-column label="当前价" width="110" align="right">
          <template #default="{ row }">
            <span v-if="row.price" class="price-text">¥{{ row.price }}</span>
            <el-tag v-else size="small" type="info">暂无报价</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="80" align="center" />
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '上架' : '下架' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" align="center" fixed="right">
          <template #default="{ row }">
            <el-button v-permission="'recycle:sku:update'" link type="primary" @click="openDialog(row)">
              编辑
            </el-button>
            <el-button
              v-permission="'recycle:sku:update'"
              link
              :type="row.status === 1 ? 'warning' : 'success'"
              @click="handleStatusChange(row)"
            >
              {{ row.status === 1 ? '下架' : '上架' }}
            </el-button>
            <el-button v-permission="'recycle:sku:delete'" link type="danger" @click="handleDelete(row)">
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
          @size-change="handleSearch"
          @current-change="loadData"
        />
      </div>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="editId ? '编辑品类' : '新增品类'"
      width="520px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="所属分类" prop="categoryId">
          <el-tree-select
            v-model="form.categoryId"
            :data="categoryTree"
            :props="{ label: 'name', value: 'id', children: 'children' }"
            node-key="id"
            check-strictly
            default-expand-all
            placeholder="请选择分类"
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="品类名称" prop="name">
          <el-input v-model.trim="form.name" placeholder="如:黄板纸(干净无胶带)" maxlength="40" />
        </el-form-item>
        <el-form-item label="图片 URL">
          <el-input v-model.trim="form.image" placeholder="https://..." />
        </el-form-item>
        <el-form-item label="单位" prop="unit">
          <el-input v-model.trim="form.unit" placeholder="kg" style="width: 120px" />
        </el-form-item>
        <el-form-item label="初始价" prop="price">
          <el-input v-model.trim="form.price" placeholder="如 0.85(元/单位)" style="width: 200px" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" :max="9999" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">上架</el-radio>
            <el-radio :value="0">下架</el-radio>
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

<style scoped lang="scss">
.price-text {
  color: #e6a23c;
  font-weight: 600;
}
</style>
