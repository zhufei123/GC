<script setup lang="ts">
</script>

<template>
  <main class="app-main">
    <router-view v-slot="{ Component }">
      <transition name="fade-slide" mode="out-in">
        <component :is="Component" :key="$route.path" />
      </transition>
    </router-view>
  </main>
</template>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.app-main {
  flex: 1;
  min-height: 0;
  overflow-y: auto;
  background-color: $page-bg;
}
</style>
