<script setup lang="ts">
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox, type FormInstance, type FormRules } from 'element-plus'

import {
  createAdmin,
  deleteAdmin,
  getAdminPage,
  getRoleList,
  resetAdminPassword,
  updateAdmin,
  updateAdminStatus,
  type AdminVO,
  type RoleVO,
} from '@/api/system'

const loading = ref(false)
const list = ref<AdminVO[]>([])
const total = ref(0)
const roleOptions = ref<RoleVO[]>([])

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
    const data = await getAdminPage(params)
    list.value = data?.list ?? []
    total.value = data?.total ?? 0
  } catch {
    // 拦截器已提示
  } finally {
    loading.value = false
  }
}

async function loadRoles(): Promise<void> {
  try {
    const data = await getRoleList({ pageNum: 1, pageSize: 100 })
    roleOptions.value = Array.isArray(data) ? data : (data?.list ?? [])
  } catch {
    // 仅影响角色下拉
  }
}

function handleSearch(): void {
  query.pageNum = 1
  loadData()
}

onMounted(() => {
  loadData()
  loadRoles()
})

function roleNames(row: AdminVO): string[] {
  if (row.roleNames?.length) return row.roleNames
  if (row.roles?.length) {
    return row.roles.map((r) => {
      if (typeof r === 'string') return r
      return r.name ?? r.id
    })
  }
  if (row.roleIds?.length) {
    return row.roleIds.map(
      (id) => roleOptions.value.find((r) => r.id === id)?.name ?? id,
    )
  }
  return []
}

/* ---------------- 新增/编辑弹窗 ---------------- */
const dialogVisible = ref(false)
const submitting = ref(false)
const editId = ref('')
const formRef = ref<FormInstance>()

const form = reactive({
  username: '',
  nickname: '',
  password: '',
  phone: '',
  roleIds: [] as string[],
  status: 1,
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  password: [
    {
      validator: (_rule, value: string, callback) => {
        if (!editId.value && !value) {
          callback(new Error('新增时必须设置初始密码'))
        } else if (value && value.length < 6) {
          callback(new Error('密码至少 6 位'))
        } else {
          callback()
        }
      },
      trigger: 'blur',
    },
  ],
  roleIds: [{ required: true, message: '请选择角色', trigger: 'change' }],
}

function openDialog(row?: AdminVO): void {
  editId.value = row?.id ?? ''
  form.username = row?.username ?? ''
  form.nickname = row?.nickname ?? ''
  form.password = ''
  form.phone = row?.phone ?? ''
  form.roleIds = row?.roleIds ? [...row.roleIds] : []
  form.status = row?.status ?? 1
  dialogVisible.value = true
}

async function submitForm(): Promise<void> {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  submitting.value = true
  try {
    const payload = {
      username: form.username,
      nickname: form.nickname,
      phone: form.phone || undefined,
      roleIds: form.roleIds,
      status: form.status,
      ...(form.password ? { password: form.password } : {}),
    }
    if (editId.value) {
      await updateAdmin(editId.value, payload)
      ElMessage.success('修改成功')
    } else {
      await createAdmin(payload)
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

async function handleStatusChange(row: AdminVO): Promise<void> {
  const target = row.status === 1 ? 0 : 1
  await updateAdminStatus(row.id, target)
  row.status = target
  ElMessage.success(target === 1 ? '已启用' : '已停用')
}

async function handleResetPwd(row: AdminVO): Promise<void> {
  let password = ''
  try {
    const result = await ElMessageBox.prompt(
      `重置管理员「${row.username}」的登录密码`,
      '重置密码',
      {
        inputType: 'password',
        inputPlaceholder: '请输入新密码(至少 6 位)',
        inputValidator: (v: string) => (v && v.length >= 6 ? true : '密码至少 6 位'),
      },
    )
    password = result.value
  } catch {
    return
  }
  await resetAdminPassword(row.id, password)
  ElMessage.success('密码已重置')
}

async function handleDelete(row: AdminVO): Promise<void> {
  try {
    await ElMessageBox.confirm(`确定删除管理员「${row.username}」吗?`, '提示', {
      type: 'warning',
    })
  } catch {
    return
  }
  await deleteAdmin(row.id)
  ElMessage.success('删除成功')
  loadData()
}
</script>

<template>
  <div class="page-container">
    <el-card class="filter-card" shadow="never">
      <el-form inline>
        <el-form-item label="关键词">
          <el-input
            v-model.trim="query.keyword"
            placeholder="用户名 / 昵称"
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
        <span class="table-title">管理员账号</span>
        <el-button v-permission="'system:admin:add'" type="primary" :icon="'Plus'" @click="openDialog()">
          新增管理员
        </el-button>
      </div>

      <el-table v-loading="loading" :data="list" border stripe>
        <el-table-column prop="username" label="用户名" min-width="120" />
        <el-table-column label="昵称" min-width="120">
          <template #default="{ row }">{{ row.nickname || '-' }}</template>
        </el-table-column>
        <el-table-column label="手机号" width="130">
          <template #default="{ row }">{{ row.phone || '-' }}</template>
        </el-table-column>
        <el-table-column label="角色" min-width="150">
          <template #default="{ row }">
            <template v-if="roleNames(row).length">
              <el-tag v-for="name in roleNames(row)" :key="name" size="small" class="role-tag">
                {{ name }}
              </el-tag>
            </template>
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
        <el-table-column prop="createdAt" label="创建时间" width="170">
          <template #default="{ row }">{{ row.createdAt || row.createTime || '-' }}</template>
        </el-table-column>
        <el-table-column label="操作" width="260" align="center" fixed="right">
          <template #default="{ row }">
            <el-button v-permission="'system:admin:update'" link type="primary" @click="openDialog(row)">
              编辑
            </el-button>
            <el-button
              v-permission="'system:admin:update'"
              link
              :type="row.status === 1 ? 'warning' : 'success'"
              @click="handleStatusChange(row)"
            >
              {{ row.status === 1 ? '停用' : '启用' }}
            </el-button>
            <el-button v-permission="'system:admin:resetPwd'" link @click="handleResetPwd(row)">
              重置密码
            </el-button>
            <el-button v-permission="'system:admin:delete'" link type="danger" @click="handleDelete(row)">
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
      :title="editId ? '编辑管理员' : '新增管理员'"
      width="520px"
      destroy-on-close
    >
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-form-item label="用户名" prop="username">
          <el-input v-model.trim="form.username" :disabled="!!editId" maxlength="30" />
        </el-form-item>
        <el-form-item label="昵称" prop="nickname">
          <el-input v-model.trim="form.nickname" maxlength="30" />
        </el-form-item>
        <el-form-item :label="editId ? '新密码' : '初始密码'" prop="password">
          <el-input
            v-model="form.password"
            type="password"
            show-password
            :placeholder="editId ? '留空则不修改密码' : '如 Init@123'"
          />
        </el-form-item>
        <el-form-item label="手机号">
          <el-input v-model.trim="form.phone" maxlength="20" />
        </el-form-item>
        <el-form-item label="角色" prop="roleIds">
          <el-select v-model="form.roleIds" multiple placeholder="请选择角色" style="width: 100%">
            <el-option v-for="role in roleOptions" :key="role.id" :label="role.name" :value="role.id" />
          </el-select>
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

<style scoped lang="scss">
.role-tag {
  margin-right: 6px;
}
</style>
