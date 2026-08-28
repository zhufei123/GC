<script setup lang="ts">
import { computed } from 'vue'
import { useRoute } from 'vue-router'

import { useAppStore } from '@/store/modules/app'
import { usePermissionStore } from '@/store/modules/permission'
import MenuItem from './MenuItem.vue'

const route = useRoute()
const appStore = useAppStore()
const permissionStore = usePermissionStore()

const activePath = computed(() => route.path)
</script>

<template>
  <div class="side-menu">
    <div class="logo">
      <el-icon class="logo-icon" :size="26"><Refresh /></el-icon>
      <transition name="fade">
        <span v-show="!appStore.sidebarCollapsed" class="logo-text">绿色回收</span>
      </transition>
    </div>
    <el-scrollbar class="menu-scroll">
      <el-menu
        class="menu"
        :default-active="activePath"
        :collapse="appStore.sidebarCollapsed"
        :collapse-transition="false"
        unique-opened
        router
      >
        <MenuItem
          v-for="menu in permissionStore.sidebarMenus"
          :key="menu.id"
          :menu="menu"
          base-path=""
        />
      </el-menu>
    </el-scrollbar>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.side-menu {
  display: flex;
  flex-direction: column;
  height: 100%;
}

.logo {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  height: $navbar-height;
  flex-shrink: 0;
  color: #fff;
  overflow: hidden;
  white-space: nowrap;

  .logo-icon {
    color: $primary-color;
  }

  .logo-text {
    font-size: 18px;
    font-weight: 700;
    letter-spacing: 2px;
  }
}

.menu-scroll {
  flex: 1;
  min-height: 0;
}

.menu {
  border-right: none;
  --el-menu-bg-color: #{$sidebar-bg};
  --el-menu-text-color: #{$sidebar-text};
  --el-menu-active-color: #{$sidebar-active-text};
  --el-menu-hover-bg-color: rgba(7, 193, 96, 0.12);

  :deep(.el-menu) {
    --el-menu-bg-color: #{$sidebar-sub-bg};
  }

  :deep(.el-menu-item.is-active) {
    background: linear-gradient(90deg, rgba(7, 193, 96, 0.9), rgba(7, 193, 96, 0.55));
    color: #fff;
  }
}

.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.2s ease;
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}
</style>
