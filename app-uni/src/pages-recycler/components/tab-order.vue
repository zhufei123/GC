<template>
  <view class="border">
    <view class="border__header">
      <view class="border__title">订单管理</view>
    </view>
    <wd-tabs v-model="activeTab" @change="onTabChange">
      <wd-tab v-for="(t, i) in STATUS_TABS" :key="t.label" :name="i" :title="t.label" />
    </wd-tabs>

    <scroll-view scroll-y class="border__list" @scrolltolower="loadMore">
      <view v-for="item in list" :key="item.id" class="border-card" @tap="goDetail(item)">
        <view class="border-card__top">
          <text class="border-card__no">单号 {{ item.orderNo || item.id }}</text>
          <wd-tag :type="statusType(item.status)" plain>{{ statusText(item.status) }}</wd-tag>
        </view>
        <view class="border-card__row">
          <wd-icon name="clock" size="28rpx" color="#86909c" />
          <text>{{ item.appointDate || "-" }} {{ item.appointPeriod || "" }}</text>
        </view>
        <view v-if="item.address" class="border-card__row">
          <wd-icon name="location" size="28rpx" color="#86909c" />
          <text class="border-card__addr">{{ item.address }}</text>
        </view>
        <view class="border-card__bottom">
          <view class="border-card__amount">
            <template v-if="item.status === 'WEIGHED' || item.status === 'COMPLETED'">
              实收 <text class="border-card__money">¥{{ item.actualAmount || "0.00" }}</text>
            </template>
            <template v-else>
              预估 <text class="border-card__money">¥{{ item.estimateAmount || "0.00" }}</text>
            </template>
          </view>
          <view class="border-card__actions">
            <wd-button
              v-if="item.status === 'ACCEPTED'"
              size="small"
              type="primary"
              plain
              @click.stop="onStart(item)"
            >
              开始服务
            </wd-button>
            <wd-button
              v-if="item.status === 'SERVING'"
              size="small"
              type="primary"
              @click.stop="goHandle(item)"
            >
              称重录入
            </wd-button>
            <wd-button
              v-if="item.status === 'WEIGHED'"
              size="small"
              type="warning"
              @click.stop="goHandle(item)"
            >
              收款完成
            </wd-button>
          </view>
        </view>
      </view>

      <wd-status-tip
        v-if="!loading && !list.length"
        image="content"
        tip="暂无订单，去接单大厅抢单吧"
      />
      <view v-if="loading" class="border__loading"><wd-loading color="#07c160" /></view>
      <view v-else-if="finished && list.length" class="border__finished">— 没有更多了 —</view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { getBossOrderPage, startService } from "@/api/boss";
import type { OrderVO } from "@/api/order";
import { statusText, statusType } from "@/utils/order-status";

const STATUS_TABS = [
  { label: "全部", value: "ALL" },
  { label: "已接单", value: "ACCEPTED" },
  { label: "服务中", value: "SERVING" },
  { label: "已称重", value: "WEIGHED" },
  { label: "已完成", value: "COMPLETED" },
];

const activeTab = ref(0);
const list = ref<OrderVO[]>([]);
const loading = ref(false);
const finished = ref(false);
let pageNum = 1;
const pageSize = 10;
let loadSeq = 0;

async function load(reset = false) {
  if (loading.value && !reset) return;
  if (reset) {
    pageNum = 1;
    finished.value = false;
    list.value = [];
  } else if (finished.value) {
    return;
  }
  const reqSeq = ++loadSeq;
  loading.value = true;
  try {
    const tab = STATUS_TABS[activeTab.value] || STATUS_TABS[0];
    const status = tab.value && tab.value !== "ALL" ? tab.value : undefined;
    const res = await getBossOrderPage({
      pageNum,
      pageSize,
      status,
    });
    if (reqSeq !== loadSeq) return;
    const rows = res?.list || [];
    list.value = pageNum === 1 ? rows : list.value.concat(rows);
    finished.value = rows.length < pageSize;
    pageNum += 1;
  } catch (e) {
    if (reqSeq === loadSeq) finished.value = true;
  } finally {
    if (reqSeq === loadSeq) loading.value = false;
  }
}

function refresh() {
  load(true);
}

function loadMore() {
  load();
}

function onTabChange(e: { index?: number; name?: number | string }) {
  const idx = typeof e?.index === "number" ? e.index : Number(e?.name);
  if (!Number.isNaN(idx) && idx >= 0) {
    activeTab.value = idx;
  }
  load(true);
}

function goDetail(item: OrderVO) {
  uni.navigateTo({ url: `/pages-recycler/order/detail?id=${item.id}` });
}

function goHandle(item: OrderVO) {
  uni.navigateTo({ url: `/pages-recycler/order/handle?id=${item.id}` });
}

async function onStart(item: OrderVO) {
  try {
    await startService(item.id);
    uni.showToast({ title: "已开始服务", icon: "success" });
    load(true);
  } catch (e) {
    /* 错误提示已由 request 统一处理 */
  }
}

defineExpose({ refresh });
</script>

<style lang="scss" scoped>
.border {
  &__header {
    background: #fff;
    padding: 28rpx 32rpx 8rpx;
  }

  &__title {
    font-size: 36rpx;
    font-weight: 700;
  }

  &__list {
    height: calc(100vh - 100rpx - 200rpx);
    padding: 24rpx 32rpx;
    box-sizing: border-box;
  }

  &__loading {
    display: flex;
    justify-content: center;
    padding: 40rpx 0;
  }

  &__finished {
    text-align: center;
    color: #c0c4cc;
    font-size: 22rpx;
    padding: 24rpx 0;
  }
}

.border-card {
  background: #fff;
  border-radius: 24rpx;
  padding: 28rpx;
  margin-bottom: 24rpx;

  &__top {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding-bottom: 20rpx;
    border-bottom: 1rpx solid #f2f3f5;
  }

  &__no {
    font-size: 26rpx;
    color: #4e5969;
  }

  &__row {
    display: flex;
    align-items: center;
    gap: 12rpx;
    margin-top: 18rpx;
    font-size: 26rpx;
    color: #4e5969;
  }

  &__addr {
    flex: 1;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__bottom {
    margin-top: 22rpx;
    display: flex;
    align-items: center;
    justify-content: space-between;
  }

  &__amount {
    font-size: 24rpx;
    color: #86909c;
  }

  &__money {
    color: #ff4d4f;
    font-size: 32rpx;
    font-weight: 700;
  }

  &__actions {
    display: flex;
    gap: 16rpx;
  }
}
</style>
