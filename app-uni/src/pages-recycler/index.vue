<template>
  <view class="shell">
    <tab-workbench v-show="active === 0" ref="workbenchRef" @switch-tab="switchTab" />
    <tab-hall v-show="active === 1" ref="hallRef" />
    <tab-order v-show="active === 2" ref="orderRef" />
    <tab-mine v-show="active === 3" ref="mineRef" />
    <app-tabbar :tabs="RECYCLER_TABS" :active="active" @change="switchTab" />
  </view>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { onLoad, onShow } from "@dcloudio/uni-app";
import { RECYCLER_TABS } from "@/config/tabbar";
import TabWorkbench from "./components/tab-workbench.vue";
import TabHall from "./components/tab-hall.vue";
import TabOrder from "./components/tab-order.vue";
import TabMine from "./components/tab-mine.vue";

const active = ref(0);
const workbenchRef = ref();
const hallRef = ref();
const orderRef = ref();
const mineRef = ref();

const refs = [workbenchRef, hallRef, orderRef, mineRef];

function switchTab(index: number) {
  active.value = index;
  refs[index].value?.refresh?.();
}

onLoad((options) => {
  const tab = Number(options?.tab);
  if (!Number.isNaN(tab) && tab >= 0 && tab <= 3) {
    active.value = tab;
  }
});

onShow(() => {
  refs[active.value].value?.refresh?.();
});
</script>

<style lang="scss" scoped>
.shell {
  min-height: 100vh;
  background: $page-bg;
}
</style>
