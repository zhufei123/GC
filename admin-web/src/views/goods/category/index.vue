<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'

import {
  createCategory,
  deleteCategory,
  getCategoryTree,
  updateCategory,
  updateCategoryStatus,
  type CategoryVO,
} from '@/api/goods'

const loading = ref(false)
const treeData = ref<CategoryVO[]>([])

async function loadData(): Promise<void> {
  loading.value = true
  try {
    treeData.value = (await getCategoryTree()) ?? []
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
  parentId: '0',
  name: '',
  icon: '',
  sort: 0,
  status: 1,
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入分类名称', trigger: 'blur' }],
}

function openDialog(row?: CategoryVO, parent?: CategoryVO): void {
  editId.value = row?.id ?? ''
  form.parentId = row?.parentId ?? parent?.id ?? '0'
  form.name = row?.name ?? ''
  form.icon = row?.icon ?? ''
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
      await updateCategory(editId.value, { ...form })
      ElMessage.success('修改成功')
    } else {
      await createCategory({ ...form })
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

async function handleDelete(row: CategoryVO): Promise<void> {
  try {
    await ElMessageBox.confirm(`确定删除分类「${row.name}」吗?`, '提示', { type: 'warning' })
  } catch {
    return
  }
  await deleteCategory(row.id)
  ElMessage.success('删除成功')
  loadData()
}

async function handleStatusChange(row: CategoryVO): Promise<void> {
  const target = row.status === 1 ? 0 : 1
  try {
    await updateCategoryStatus(row.id, target)
    row.status = target
    ElMessage.success(target === 1 ? '已启用' : '已停用')
  } catch {
    // 保持原状态
  }
}
</script>

<template>
  <div class="page-container">
    <el-card class="table-card" shadow="never">
      <div class="table-toolbar">
        <span class="table-title">回收分类</span>
        <div>
          <el-button :icon="'Refresh'" circle @click="loadData" />
          <el-button
            v-permission="'recycle:category:add'"
            type="primary"
            :icon="'Plus'"
            @click="openDialog()"
          >
            新增分类
          </el-button>
        </div>
      </div>

      <el-table
        v-loading="loading"
        :data="treeData"
        row-key="id"
        border
        default-expand-all
        :tree-props="{ children: 'children' }"
      >
        <el-table-column prop="name" label="分类名称" min-width="200" />
        <el-table-column label="图标" width="90" align="center">
          <template #default="{ row }">
            <el-image
              v-if="row.icon"
              :src="row.icon"
              :preview-src-list="[row.icon]"
              preview-teleported
              fit="cover"
              style="width: 36px; height: 36px; border-radius: 6px"
            />
            <span v-else>-</span>
          </template>
        </el-table-column>
        <el-table-column label="层级" width="90" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="row.parentId === '0' ? 'success' : 'info'">
              {{ row.parentId === '0' ? '一级' : '二级' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="80" align="center" />
        <el-table-column label="状态" width="100" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="small">
              {{ row.status === 1 ? '启用' : '停用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="280" align="center" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.parentId === '0'"
              v-permission="'recycle:category:add'"
              link
              type="primary"
              @click="openDialog(undefined, row)"
            >
              新增子级
            </el-button>
            <el-button
              v-permission="'recycle:category:update'"
              link
              type="primary"
              @click="openDialog(row)"
            >
              编辑
            </el-button>
            <el-button
              v-permission="'recycle:category:update'"
              link
              :type="row.status === 1 ? 'warning' : 'success'"
              @click="handleStatusChange(row)"
            >
              {{ row.status === 1 ? '停用' : '启用' }}
            </el-button>
            <el-button
              v-permission="'recycle:category:delete'"
              link
              type="danger"
              @click="handleDelete(row)"
            >
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="editId ? '编辑分类' : '新增分类'"
      width="480px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="上级分类">
          <el-tree-select
            v-model="form.parentId"
            :data="[{ id: '0', name: '顶级分类', children: treeData }]"
            :props="{ label: 'name', value: 'id', children: 'children' }"
            node-key="id"
            check-strictly
            default-expand-all
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="分类名称" prop="name">
          <el-input v-model.trim="form.name" placeholder="如:废纸类" maxlength="20" />
        </el-form-item>
        <el-form-item label="图标 URL">
          <el-input v-model.trim="form.icon" placeholder="https://..." />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" :max="9999" />
        </el-form-item>
        <el-form-item label="状态">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
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
