<script setup lang="ts">
import { computed } from 'vue'

import { useAppStore } from '@/store/modules/app'
import AppMain from './components/AppMain.vue'
import Navbar from './components/Navbar.vue'
import SideMenu from './components/SideMenu.vue'

const appStore = useAppStore()
const asideWidth = computed(() => (appStore.sidebarCollapsed ? '64px' : '220px'))
</script>

<template>
  <el-container class="layout">
    <el-aside class="layout-aside" :width="asideWidth">
      <SideMenu />
    </el-aside>
    <el-container class="layout-body" direction="vertical">
      <Navbar />
      <AppMain />
    </el-container>
  </el-container>
</template>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.layout {
  width: 100%;
  height: 100%;
}

.layout-aside {
  background-color: $sidebar-bg;
  transition: width 0.25s ease;
  overflow-x: hidden;
}

.layout-body {
  min-width: 0;
  background-color: $page-bg;
}
</style>
