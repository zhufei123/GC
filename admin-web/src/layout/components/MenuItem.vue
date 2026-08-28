<script setup lang="ts">
import { computed } from 'vue'

import type { MenuVO } from '@/types/menu'
import { resolveIcon } from '@/utils/icon'

defineOptions({ name: 'MenuItem' })

const props = defineProps<{
  menu: MenuVO
  basePath: string
}>()

function joinPath(base: string, path: string): string {
  if (!path) return base || '/'
  if (path.startsWith('/')) return path
  return `${base.replace(/\/+$/, '')}/${path}`
}

const fullPath = computed(() => joinPath(props.basePath, props.menu.path))

const visibleChildren = computed(() =>
  (props.menu.children ?? []).filter(
    (c) => c.type !== 'BUTTON' && c.visible !== false && c.visible !== 0,
  ),
)

const isDir = computed(
  () => props.menu.type === 'DIR' && visibleChildren.value.length > 0,
)

const iconName = computed(() => resolveIcon(props.menu.icon, 'Folder'))
</script>

<template>
  <el-sub-menu v-if="isDir" :index="fullPath">
    <template #title>
      <el-icon><component :is="iconName" /></el-icon>
      <span>{{ menu.title }}</span>
    </template>
    <MenuItem
      v-for="child in visibleChildren"
      :key="child.id"
      :menu="child"
      :base-path="fullPath"
    />
  </el-sub-menu>

  <el-menu-item v-else :index="fullPath">
    <el-icon><component :is="resolveIcon(menu.icon, 'Document')" /></el-icon>
    <template #title>{{ menu.title }}</template>
  </el-menu-item>
</template>
