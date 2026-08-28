<template>
  <view class="hall">
    <view class="hall__header">
      <view>
        <view class="hall__title">接单大厅</view>
        <view class="hall__sub">附近待接订单 · 先到先得</view>
      </view>
      <view class="hall__refresh" @tap="refresh(true)">
        <wd-icon name="refresh" size="30rpx" color="#07c160" />
        <text>刷新</text>
      </view>
    </view>

    <scroll-view scroll-y class="hall__list" @scrolltolower="loadMore">
      <view v-for="item in list" :key="item.id" class="pool-card">
        <view class="pool-card__top">
          <wd-tag type="warning" plain>{{ item.type === "DROPOFF" ? "到店回收" : "上门回收" }}</wd-tag>
          <text class="pool-card__amount">预估 ¥{{ item.estimateAmount || "0.00" }}</text>
        </view>
        <view class="pool-card__row">
          <wd-icon name="clock" size="28rpx" color="#86909c" />
          <text>{{ item.appointDate || "-" }} {{ item.appointPeriod || "" }}</text>
        </view>
        <view class="pool-card__row">
          <wd-icon name="location" size="28rpx" color="#86909c" />
          <text class="pool-card__addr">{{ item.address || "地址信息接单后可见" }}</text>
        </view>
        <view v-if="itemsSummary(item)" class="pool-card__row">
          <wd-icon name="goods" size="28rpx" color="#86909c" />
          <text>{{ itemsSummary(item) }}</text>
        </view>
        <wd-button
          type="primary"
          block
          size="small"
          custom-class="pool-card__btn"
          @click="onAccept(item)"
        >
          立即抢单
        </wd-button>
      </view>

      <wd-status-tip
        v-if="!loading && !list.length"
        image="content"
        tip="附近暂无待接订单，稍后再来看看"
      />
      <view v-if="loading" class="hall__loading"><wd-loading color="#07c160" /></view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { getOrderPool, acceptOrder } from "@/api/boss";
import type { OrderVO } from "@/api/order";

const list = ref<OrderVO[]>([]);
const loading = ref(false);
const finished = ref(false);
let pageNum = 1;
const pageSize = 10;
let loadedOnce = false;

function itemsSummary(item: OrderVO) {
  if (!item.items?.length) return "";
  return item.items
    .map((it) => `${it.skuName || it.skuId}${it.estimateWeight ? " " + it.estimateWeight + "kg" : ""}`)
    .join("、");
}

async function load(reset = false) {
  if (loading.value) return;
  if (reset) {
    pageNum = 1;
    finished.value = false;
    list.value = [];
  }
  if (finished.value) return;
  loading.value = true;
  try {
    const res = await getOrderPool({ pageNum, pageSize });
    const rows = res?.list || (Array.isArray(res) ? (res as unknown as OrderVO[]) : []);
    list.value = pageNum === 1 ? rows : list.value.concat(rows);
    finished.value = rows.length < pageSize;
    pageNum += 1;
  } catch (e) {
    finished.value = true;
  } finally {
    loading.value = false;
  }
}

function refresh(force = false) {
  if (loadedOnce && !force) {
    load(true);
    return;
  }
  loadedOnce = true;
  load(true);
}

function loadMore() {
  load();
}

function onAccept(item: OrderVO) {
  uni.showModal({
    title: "确认抢单",
    content: `预估金额 ¥${item.estimateAmount || "0.00"}，确定接下这单吗？`,
    success: async (res) => {
      if (!res.confirm) return;
      try {
        await acceptOrder(item.id);
        uni.showToast({ title: "接单成功", icon: "success" });
        load(true);
      } catch (e) {
        // 20403 已被抢等场景：request 已 toast，这里刷新列表
        load(true);
      }
    },
  });
}

refresh();
defineExpose({ refresh });
</script>

<style lang="scss" scoped>
.hall {
  &__header {
    background: #fff;
    padding: 28rpx 32rpx;
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  &__title {
    font-size: 36rpx;
    font-weight: 700;
  }

  &__sub {
    margin-top: 6rpx;
    font-size: 22rpx;
    color: #86909c;
  }

  &__refresh {
    display: flex;
    align-items: center;
    gap: 8rpx;
    color: #07c160;
    font-size: 26rpx;
    background: $theme-color-light;
    padding: 12rpx 24rpx;
    border-radius: 32rpx;
  }

  &__list {
    height: calc(100vh - 100rpx - 140rpx);
    padding: 24rpx 32rpx;
    box-sizing: border-box;
  }

  &__loading {
    display: flex;
    justify-content: center;
    padding: 40rpx 0;
  }
}

.pool-card {
  background: #fff;
  border-radius: 24rpx;
  padding: 28rpx;
  margin-bottom: 24rpx;

  &__top {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding-bottom: 18rpx;
    border-bottom: 1rpx solid #f2f3f5;
  }

  &__amount {
    color: #ff4d4f;
    font-size: 30rpx;
    font-weight: 700;
  }

  &__row {
    display: flex;
    align-items: center;
    gap: 12rpx;
    margin-top: 16rpx;
    font-size: 26rpx;
    color: #4e5969;
  }

  &__addr {
    flex: 1;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  :deep(.pool-card__btn) {
    margin-top: 24rpx;
    border-radius: 40rpx !important;
  }
}
</style>
