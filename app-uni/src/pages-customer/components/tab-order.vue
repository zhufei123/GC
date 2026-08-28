<template>
  <view class="order">
    <view class="order__header">
      <view class="order__title">我的订单</view>
    </view>
    <wd-tabs v-model="activeTab" @change="onTabChange" custom-class="order__tabs">
      <wd-tab v-for="t in STATUS_TABS" :key="t.value" :title="t.label" />
    </wd-tabs>

    <scroll-view scroll-y class="order__list" @scrolltolower="loadMore">
      <view v-for="item in list" :key="item.id" class="order-card" @tap="goDetail(item)">
        <view class="order-card__top">
          <text class="order-card__no">单号 {{ item.orderNo || item.id }}</text>
          <wd-tag :type="statusType(item.status)" plain>{{ statusText(item.status) }}</wd-tag>
        </view>
        <view class="order-card__row">
          <wd-icon name="clock" size="28rpx" color="#86909c" />
          <text>{{ item.appointDate || "-" }} {{ item.appointPeriod || "" }}</text>
        </view>
        <view v-if="item.address" class="order-card__row">
          <wd-icon name="location" size="28rpx" color="#86909c" />
          <text class="order-card__addr">{{ item.address }}</text>
        </view>
        <view class="order-card__bottom">
          <view class="order-card__amount">
            <template v-if="item.status === 'COMPLETED' || item.status === 'WEIGHED'">
              实收 <text class="order-card__money">¥{{ item.actualAmount || "0.00" }}</text>
            </template>
            <template v-else>
              预估 <text class="order-card__money">¥{{ item.estimateAmount || "0.00" }}</text>
            </template>
          </view>
          <view
            v-if="item.status === 'PENDING' || item.status === 'ACCEPTED'"
            class="order-card__cancel"
            @tap.stop="onCancel(item)"
          >
            取消订单
          </view>
        </view>
      </view>

      <wd-status-tip
        v-if="!loading && !list.length"
        image="content"
        tip="还没有回收订单，去首页预约上门回收吧"
      />
      <view v-if="loading" class="order__loading"><wd-loading color="#07c160" /></view>
      <view v-else-if="finished && list.length" class="order__finished">— 没有更多了 —</view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { getOrderPage, cancelOrder } from "@/api/order";
import type { OrderVO } from "@/api/order";
import { statusText, statusType } from "@/utils/order-status";

const STATUS_TABS = [
  { label: "全部", value: "" },
  { label: "待接单", value: "PENDING" },
  { label: "已接单", value: "ACCEPTED" },
  { label: "服务中", value: "SERVING" },
  { label: "已称重", value: "WEIGHED" },
  { label: "已完成", value: "COMPLETED" },
  { label: "已取消", value: "CANCELLED" },
];

const activeTab = ref(0);
const list = ref<OrderVO[]>([]);
const loading = ref(false);
const finished = ref(false);
let pageNum = 1;
const pageSize = 10;

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
    const status = STATUS_TABS[activeTab.value].value;
    const res = await getOrderPage({
      pageNum,
      pageSize,
      ...(status ? { status } : {}),
    });
    const rows = res?.list || [];
    list.value = pageNum === 1 ? rows : list.value.concat(rows);
    finished.value = rows.length < pageSize;
    pageNum += 1;
  } catch (e) {
    finished.value = true;
  } finally {
    loading.value = false;
  }
}

function refresh() {
  load(true);
}

function loadMore() {
  load();
}

function onTabChange() {
  load(true);
}

function goDetail(item: OrderVO) {
  uni.navigateTo({ url: `/pages-customer/order/detail?id=${item.id}` });
}

function onCancel(item: OrderVO) {
  uni.showModal({
    title: "取消订单",
    editable: true,
    placeholderText: "请填写取消原因",
    success: async (res) => {
      if (!res.confirm) return;
      const reason = (res as any).content || "用户主动取消";
      try {
        await cancelOrder(item.id, reason);
        uni.showToast({ title: "已取消", icon: "success" });
        load(true);
      } catch (e) {
        /* 错误提示已由 request 统一处理 */
      }
    },
  });
}

defineExpose({ refresh });
</script>

<style lang="scss" scoped>
.order {
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

.order-card {
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

  &__cancel {
    font-size: 24rpx;
    color: #86909c;
    border: 1rpx solid #dcdfe6;
    border-radius: 28rpx;
    padding: 8rpx 24rpx;
  }
}
</style>
