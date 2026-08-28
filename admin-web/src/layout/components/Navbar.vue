<script setup lang="ts">
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessageBox } from 'element-plus'

import { useAppStore } from '@/store/modules/app'
import { useUserStore } from '@/store/modules/user'

const route = useRoute()
const router = useRouter()
const appStore = useAppStore()
const userStore = useUserStore()

const breadcrumbs = computed(() =>
  route.matched.filter((item) => item.meta?.title && item.path !== '/'),
)

const avatarText = computed(() => userStore.nickname.slice(0, 1).toUpperCase())

async function handleCommand(command: string): Promise<void> {
  if (command === 'dashboard') {
    router.push('/')
    return
  }
  if (command === 'logout') {
    try {
      await ElMessageBox.confirm('确定退出登录吗?', '提示', {
        type: 'warning',
        confirmButtonText: '退出',
        cancelButtonText: '取消',
      })
    } catch {
      return
    }
    await userStore.logoutAction()
    // 整页跳转,顺带清空 pinia 与动态路由
    location.href = '/login'
  }
}
</script>

<template>
  <header class="navbar">
    <div class="navbar-left">
      <el-icon class="collapse-btn" :size="20" @click="appStore.toggleSidebar()">
        <Expand v-if="appStore.sidebarCollapsed" />
        <Fold v-else />
      </el-icon>
      <el-breadcrumb separator="/">
        <el-breadcrumb-item v-for="item in breadcrumbs" :key="item.path">
          {{ item.meta.title }}
        </el-breadcrumb-item>
      </el-breadcrumb>
    </div>

    <div class="navbar-right">
      <el-dropdown trigger="click" @command="handleCommand">
        <div class="user-info">
          <el-avatar v-if="userStore.admin?.avatar" :size="32" :src="userStore.admin.avatar" />
          <el-avatar v-else :size="32" class="avatar-fallback">{{ avatarText }}</el-avatar>
          <span class="nickname">{{ userStore.nickname }}</span>
          <el-icon><ArrowDown /></el-icon>
        </div>
        <template #dropdown>
          <el-dropdown-menu>
            <el-dropdown-item command="dashboard">
              <el-icon><HomeFilled /></el-icon>回到首页
            </el-dropdown-item>
            <el-dropdown-item divided command="logout">
              <el-icon><SwitchButton /></el-icon>退出登录
            </el-dropdown-item>
          </el-dropdown-menu>
        </template>
      </el-dropdown>
    </div>
  </header>
</template>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.navbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: $navbar-height;
  flex-shrink: 0;
  padding: 0 16px;
  background: #fff;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  position: relative;
  z-index: 5;
}

.navbar-left {
  display: flex;
  align-items: center;
  gap: 14px;

  .collapse-btn {
    cursor: pointer;
    color: #555;

    &:hover {
      color: $primary-color;
    }
  }
}

.navbar-right {
  .user-info {
    display: flex;
    align-items: center;
    gap: 8px;
    cursor: pointer;
    padding: 4px 8px;
    border-radius: 8px;

    &:hover {
      background: #f5f7f6;
    }

    .avatar-fallback {
      background: $primary-color;
      color: #fff;
      font-weight: 600;
    }

    .nickname {
      font-size: 14px;
      color: #333;
      max-width: 120px;
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }
}
</style>
