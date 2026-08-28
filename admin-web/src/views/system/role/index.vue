<script setup lang="ts">
import { nextTick, onMounted, reactive, ref } from 'vue'
import {
  ElMessage,
  ElMessageBox,
  type ElTree,
  type FormInstance,
  type FormRules,
} from 'element-plus'

import {
  assignRoleMenus,
  createRole,
  deleteRole,
  getMenuTree,
  getRoleList,
  getRoleMenus,
  updateRole,
  type RoleVO,
} from '@/api/system'
import type { MenuVO } from '@/types/menu'

const loading = ref(false)
const list = ref<RoleVO[]>([])

async function loadData(): Promise<void> {
  loading.value = true
  try {
    const data = await getRoleList({ pageNum: 1, pageSize: 100 })
    list.value = Array.isArray(data) ? data : (data?.list ?? [])
  } catch {
    // 拦截器已提示
  } finally {
    loading.value = false
  }
}

onMounted(loadData)

/* ---------------- 角色弹窗 ---------------- */
const dialogVisible = ref(false)
const submitting = ref(false)
const editId = ref('')
const formRef = ref<FormInstance>()

const form = reactive({
  name: '',
  code: '',
  remark: '',
  status: 1,
})

const rules: FormRules = {
  name: [{ required: true, message: '请输入角色名称', trigger: 'blur' }],
  code: [{ required: true, message: '请输入角色编码', trigger: 'blur' }],
}

function openDialog(row?: RoleVO): void {
  editId.value = row?.id ?? ''
  form.name = row?.name ?? ''
  form.code = row?.code ?? ''
  form.remark = row?.remark ?? ''
  form.status = row?.status ?? 1
  dialogVisible.value = true
}

async function submitForm(): Promise<void> {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    if (editId.value) {
      await updateRole(editId.value, { ...form })
      ElMessage.success('修改成功')
    } else {
      await createRole({ ...form })
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

async function handleDelete(row: RoleVO): Promise<void> {
  try {
    await ElMessageBox.confirm(`确定删除角色「${row.name}」吗?`, '提示', { type: 'warning' })
  } catch {
    return
  }
  await deleteRole(row.id)
  ElMessage.success('删除成功')
  loadData()
}

/* ---------------- 权限树抽屉 ---------------- */
const drawerVisible = ref(false)
const treeLoading = ref(false)
const assigning = ref(false)
const menuTree = ref<MenuVO[]>([])
const currentRole = ref<RoleVO | null>(null)
const treeRef = ref<InstanceType<typeof ElTree>>()

async function openAssign(row: RoleVO): Promise<void> {
  currentRole.value = row
  drawerVisible.value = true
  treeLoading.value = true
  try {
    const [tree, assigned] = await Promise.all([getMenuTree(), getRoleMenus(row.id)])
    menuTree.value = tree ?? []
    const checkedIds = Array.isArray(assigned) ? assigned : (assigned?.menuIds ?? [])
    // 只勾选叶子,父级由 el-tree 半选推导,避免全选副作用
    const leafIds = collectLeafIds(menuTree.value, new Set(checkedIds.map(String)))
    await nextTick()
    treeRef.value?.setCheckedKeys(leafIds)
  } catch {
    // 拦截器已提示
  } finally {
    treeLoading.value = false
  }
}

function collectLeafIds(nodes: MenuVO[], checked: Set<string>): string[] {
  const result: string[] = []
  const walk = (items: MenuVO[]) => {
    for (const item of items) {
      const children = item.children ?? []
      if (children.length) {
        walk(children)
      } else if (checked.has(String(item.id))) {
        result.push(item.id)
      }
    }
  }
  walk(nodes)
  return result
}

async function submitAssign(): Promise<void> {
  if (!currentRole.value || !treeRef.value) return
  const checked = treeRef.value.getCheckedKeys(false) as Array<string | number>
  const halfChecked = treeRef.value.getHalfCheckedKeys() as Array<string | number>
  const menuIds = [...checked, ...halfChecked].map(String)

  assigning.value = true
  try {
    await assignRoleMenus(currentRole.value.id, menuIds)
    ElMessage.success('权限已保存')
    drawerVisible.value = false
  } catch {
    // 拦截器已提示
  } finally {
    assigning.value = false
  }
}

const MENU_TYPE_TAG: Record<string, string> = {
  DIR: '目录',
  MENU: '菜单',
  BUTTON: '按钮',
}
</script>

<template>
  <div class="page-container">
    <el-card class="table-card" shadow="never">
      <div class="table-toolbar">
        <span class="table-title">角色管理</span>
        <div>
          <el-button :icon="'Refresh'" circle @click="loadData" />
          <el-button v-permission="'system:role:add'" type="primary" :icon="'Plus'" @click="openDialog()">
            新增角色
          </el-button>
        </div>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="name" label="角色名称" min-width="140" />
        <el-table-column label="角色编码" min-width="140">
          <template #default="{ row }">
            <el-tag size="small" type="info" effect="plain">{{ row.code || '-' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="备注" min-width="180" show-overflow-tooltip>
          <template #default="{ row }">{{ row.remark || '-' }}</template>
        </el-table-column>
        <el-table-column label="状态" width="90" align="center">
          <template #default="{ row }">
            <el-tag :type="row.status === 0 ? 'danger' : 'success'" size="small">
              {{ row.status === 0 ? '停用' : '启用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" width="170">
          <template #default="{ row }">{{ row.createdAt || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="220" align="center" fixed="right">
          <template #default="{ row }">
            <el-button v-permission="'system:role:assign'" link type="success" @click="openAssign(row)">
              分配权限
            </el-button>
            <el-button v-permission="'system:role:update'" link type="primary" @click="openDialog(row)">
              编辑
            </el-button>
            <el-button v-permission="'system:role:delete'" link type="danger" @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 角色弹窗 -->
    <el-dialog
      v-model="dialogVisible"
      :title="editId ? '编辑角色' : '新增角色'"
      width="480px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="角色名称" prop="name">
          <el-input v-model.trim="form.name" placeholder="如:运营" maxlength="30" />
        </el-form-item>
        <el-form-item label="角色编码" prop="code">
          <el-input v-model.trim="form.code" placeholder="如:operator" maxlength="50" />
        </el-form-item>
        <el-form-item label="备注">
          <el-input v-model.trim="form.remark" type="textarea" :rows="2" maxlength="200" />
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

    <!-- 权限树抽屉 -->
    <el-drawer v-model="drawerVisible" :title="`分配权限 - ${currentRole?.name ?? ''}`" size="420px">
      <div v-loading="treeLoading" class="assign-tree">
        <el-tree
          ref="treeRef"
          :data="menuTree"
          node-key="id"
          show-checkbox
          default-expand-all
          :props="{ label: 'title', children: 'children' }"
        >
          <template #default="{ data }">
            <span class="tree-node">
              <span>{{ data.title || data.name }}</span>
              <el-tag size="small" effect="plain" class="tree-tag">
                {{ MENU_TYPE_TAG[data.type] ?? data.type }}
              </el-tag>
            </span>
          </template>
        </el-tree>
      </div>
      <template #footer>
        <el-button @click="drawerVisible = false">取消</el-button>
        <el-button type="primary" :loading="assigning" @click="submitAssign">保存</el-button>
      </template>
    </el-drawer>
  </div>
</template>

<style scoped lang="scss">
.assign-tree {
  min-height: 200px;
}

.tree-node {
  display: flex;
  align-items: center;
  gap: 8px;

  .tree-tag {
    transform: scale(0.85);
  }
}
</style>
