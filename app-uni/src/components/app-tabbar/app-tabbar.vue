<template>
  <wd-tabbar
    :model-value="active"
    fixed
    bordered
    safe-area-inset-bottom
    placeholder
    active-color="#07c160"
    inactive-color="#9ca3af"
    @change="onChange"
  >
    <wd-tabbar-item
      v-for="(tab, i) in tabs"
      :key="i"
      :title="tab.title"
      :icon="tab.icon"
      :value="badges && badges[i] ? badges[i] : undefined"
    />
  </wd-tabbar>
</template>

<script setup lang="ts">
import type { TabItem } from "@/config/tabbar";

defineProps<{
  tabs: TabItem[];
  active: number;
  badges?: (number | undefined)[];
}>();

const emit = defineEmits<{ (e: "change", index: number): void }>();

function onChange({ value }: { value: number }) {
  emit("change", value);
}
</script>
