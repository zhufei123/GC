<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'

import { createMenu, deleteMenu, getMenuTree, updateMenu } from '@/api/system'
import type { MenuType, MenuVO } from '@/types/menu'
import { resolveIcon } from '@/utils/icon'

const loading = ref(false)
const treeData = ref<MenuVO[]>([])

async function loadData(): Promise<void> {
  loading.value = true
  try {
    treeData.value = (await getMenuTree()) ?? []
  } catch {
    // 拦截器已提示
  } finally {
    loading.value = false
  }
}

onMounted(loadData)

type TagType = 'success' | 'warning' | 'info' | 'danger' | 'primary'

const MENU_TYPE_INFO: Record<string, { label: string; type: TagType }> = {
  DIR: { label: '目录', type: 'warning' },
  MENU: { label: '菜单', type: 'success' },
  BUTTON: { label: '按钮', type: 'info' },
}

/* ---------------- 弹窗表单 ---------------- */
const dialogVisible = ref(false)
const submitting = ref(false)
const editId = ref('')
const formRef = ref<FormInstance>()

const form = reactive({
  parentId: '0',
  type: 'MENU' as MenuType,
  title: '',
  name: '',
  path: '',
  component: '',
  icon: '',
  perms: '',
  sort: 0,
  visible: true,
})

const rules: FormRules = {
  title: [{ required: true, message: '请输入菜单标题', trigger: 'blur' }],
  // 后端 sys_menu.name NOT NULL,按钮也必须有唯一标识
  name: [{ required: true, message: '请输入唯一标识 name', trigger: 'blur' }],
  path: [
    {
      validator: (_rule, value: string, callback) => {
        if (form.type !== 'BUTTON' && !value) {
          callback(new Error('目录/菜单必须填写路由 path'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
  component: [
    {
      validator: (_rule, value: string, callback) => {
        if (form.type === 'MENU' && !value) {
          callback(new Error('菜单必须填写组件路径'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
  perms: [
    {
      validator: (_rule, value: string, callback) => {
        if (form.type === 'BUTTON' && !value) {
          callback(new Error('按钮必须填写权限码'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
}

function openDialog(row?: MenuVO, parent?: MenuVO): void {
  editId.value = row?.id ?? ''
  form.parentId = row?.parentId ?? parent?.id ?? '0'
  form.type = row?.type ?? (parent ? 'MENU' : 'DIR')
  form.title = row?.title ?? ''
  form.name = row?.name ?? ''
  form.path = row?.path ?? ''
  form.component = row?.component ?? ''
  form.icon = row?.icon ?? ''
  form.perms = row?.perms ?? ''
  form.sort = row?.sort ?? 0
  form.visible = !(row?.visible === false || row?.visible === 0)
  dialogVisible.value = true
}

async function submitForm(): Promise<void> {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const payload = {
      parentId: form.parentId,
      type: form.type,
      title: form.title,
      name: form.name,
      path: form.type === 'BUTTON' ? undefined : form.path,
      component: form.type === 'MENU' ? form.component : undefined,
      icon: form.icon || undefined,
      perms: form.perms || undefined,
      sort: form.sort,
      // 后端 MenuSaveDTO.visible 为 Integer(1显示/0隐藏)
      visible: form.visible ? 1 : 0,
    }
    if (editId.value) {
      await updateMenu(editId.value, payload)
      ElMessage.success('修改成功')
    } else {
      await createMenu(payload)
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

async function handleDelete(row: MenuVO): Promise<void> {
  try {
    await ElMessageBox.confirm(`确定删除「${row.title}」吗?`, '提示', { type: 'warning' })
  } catch {
    return
  }
  await deleteMenu(row.id)
  ElMessage.success('删除成功')
  loadData()
}
</script>

<template>
  <div class="page-container">
    <el-card class="table-card" shadow="never">
      <div class="table-toolbar">
        <span class="table-title">菜单管理</span>
        <div>
          <el-button :icon="'Refresh'" circle @click="loadData" />
          <el-button v-permission="'system:menu:add'" type="primary" :icon="'Plus'" @click="openDialog()">
            新增菜单
          </el-button>
        </div>
      </div>

      <el-table
        v-loading="loading"
        :data="treeData"
        row-key="id"
        border
        :tree-props="{ children: 'children' }"
      >
        <el-table-column prop="title" label="菜单标题" min-width="180">
          <template #default="{ row }">
            <el-icon v-if="row.type !== 'BUTTON'" class="menu-icon">
              <component :is="resolveIcon(row.icon, row.type === 'DIR' ? 'Folder' : 'Document')" />
            </el-icon>
            {{ row.title }}
          </template>
        </el-table-column>
        <el-table-column label="类型" width="80" align="center">
          <template #default="{ row }">
            <el-tag size="small" :type="MENU_TYPE_INFO[row.type]?.type ?? 'info'">
              {{ MENU_TYPE_INFO[row.type]?.label ?? row.type }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="路由 name" min-width="120">
          <template #default="{ row }">{{ row.name || '-' }}</template>
        </el-table-column>
        <el-table-column label="path" min-width="120">
          <template #default="{ row }">{{ row.path || '-' }}</template>
        </el-table-column>
        <el-table-column label="组件" min-width="170" show-overflow-tooltip>
          <template #default="{ row }">{{ row.component || '-' }}</template>
        </el-table-column>
        <el-table-column label="权限码" min-width="170" show-overflow-tooltip>
          <template #default="{ row }">{{ row.perms || '-' }}</template>
        </el-table-column>
        <el-table-column prop="sort" label="排序" width="70" align="center" />
        <el-table-column label="可见" width="70" align="center">
          <template #default="{ row }">
            <el-tag
              size="small"
              :type="row.visible === false || row.visible === 0 ? 'info' : 'success'"
            >
              {{ row.visible === false || row.visible === 0 ? '隐藏' : '显示' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="200" align="center" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.type !== 'BUTTON'"
              v-permission="'system:menu:add'"
              link
              type="primary"
              @click="openDialog(undefined, row)"
            >
              新增子级
            </el-button>
            <el-button v-permission="'system:menu:update'" link type="primary" @click="openDialog(row)">
              编辑
            </el-button>
            <el-button v-permission="'system:menu:delete'" link type="danger" @click="handleDelete(row)">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog
      v-model="dialogVisible"
      :title="editId ? '编辑菜单' : '新增菜单'"
      width="560px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="上级菜单">
          <el-tree-select
            v-model="form.parentId"
            :data="[{ id: '0', title: '顶级', children: treeData }]"
            :props="{ label: 'title', value: 'id', children: 'children' }"
            node-key="id"
            check-strictly
            default-expand-all
            style="width: 100%"
          />
        </el-form-item>
        <el-form-item label="类型">
          <el-radio-group v-model="form.type">
            <el-radio-button value="DIR">目录</el-radio-button>
            <el-radio-button value="MENU">菜单</el-radio-button>
            <el-radio-button value="BUTTON">按钮</el-radio-button>
          </el-radio-group>
        </el-form-item>
        <el-form-item label="标题" prop="title">
          <el-input v-model.trim="form.title" placeholder="如:商品管理" maxlength="30" />
        </el-form-item>
        <el-form-item :label="form.type === 'BUTTON' ? '标识 name' : '路由 name'" prop="name">
          <el-input
            v-model.trim="form.name"
            :placeholder="form.type === 'BUTTON' ? '如:CatAdd(唯一)' : '如:GoodsCategory(唯一)'"
          />
        </el-form-item>
        <el-form-item v-if="form.type !== 'BUTTON'" label="路由 path" prop="path">
          <el-input
            v-model.trim="form.path"
            placeholder="一级填 /goods,子级填 category(不带 /)"
          />
        </el-form-item>
        <el-form-item v-if="form.type === 'MENU'" label="组件路径" prop="component">
          <el-input v-model.trim="form.component" placeholder="如:goods/category/index" />
        </el-form-item>
        <el-form-item v-if="form.type !== 'BUTTON'" label="图标">
          <el-input v-model.trim="form.icon" placeholder="Element Plus 图标名,如 Goods" />
        </el-form-item>
        <el-form-item v-if="form.type === 'BUTTON'" label="权限码" prop="perms">
          <el-input v-model.trim="form.perms" placeholder="如:recycle:category:add" />
        </el-form-item>
        <el-form-item label="排序">
          <el-input-number v-model="form.sort" :min="0" :max="9999" />
        </el-form-item>
        <el-form-item v-if="form.type !== 'BUTTON'" label="是否可见">
          <el-switch v-model="form.visible" active-text="显示" inactive-text="隐藏" />
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
.menu-icon {
  vertical-align: -2px;
  margin-right: 4px;
  color: #07c160;
}
</style>
