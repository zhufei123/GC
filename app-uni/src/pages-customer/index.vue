<template>
  <view class="shell">
    <tab-home v-show="active === 0" ref="homeRef" @switch-tab="switchTab" />
    <tab-price v-show="active === 1" ref="priceRef" />
    <tab-order v-show="active === 2" ref="orderRef" />
    <tab-mine v-show="active === 3" ref="mineRef" />
    <app-tabbar :tabs="CUSTOMER_TABS" :active="active" @change="switchTab" />
  </view>
</template>

<script setup lang="ts">
import { nextTick, onMounted, ref } from "vue";
import { onLoad, onShow } from "@dcloudio/uni-app";
import { CUSTOMER_TABS } from "@/config/tabbar";
import TabHome from "./components/tab-home.vue";
import TabPrice from "./components/tab-price.vue";
import TabOrder from "./components/tab-order.vue";
import TabMine from "./components/tab-mine.vue";

const active = ref(0);
const homeRef = ref();
const priceRef = ref();
const orderRef = ref();
const mineRef = ref();

const refs = [homeRef, priceRef, orderRef, mineRef];

function switchTab(index: number, categoryId?: string) {
  active.value = index;
  if (index === 1 && categoryId) {
    priceRef.value?.selectCategory?.(categoryId);
    return;
  }
  refs[index].value?.refresh?.();
}

function refreshActive() {
  nextTick(() => {
    refs[active.value].value?.refresh?.();
  });
}

onLoad((options) => {
  const tab = Number(options?.tab);
  if (!Number.isNaN(tab) && tab >= 0 && tab <= 3) {
    active.value = tab;
  }
});

onShow(refreshActive);

onMounted(refreshActive);
</script>

<style lang="scss" scoped>
.shell {
  min-height: 100vh;
  background: $page-bg;
}
</style>
