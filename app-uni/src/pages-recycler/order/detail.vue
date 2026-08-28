<template>
  <view class="bdetail">
    <view v-if="order" class="bdetail__inner">
      <view class="status-head">
        <wd-tag :type="statusType(order.status)">{{ statusText(order.status) }}</wd-tag>
        <text class="status-head__no">单号 {{ order.orderNo || order.id }}</text>
      </view>

      <view class="card">
        <view class="card__title">客户信息</view>
        <view class="info-row">
          <wd-icon name="user" size="30rpx" color="#86909c" />
          <text>{{ order.receiver || "-" }} {{ order.phone || "" }}</text>
        </view>
        <view class="info-row">
          <wd-icon name="location" size="30rpx" color="#86909c" />
          <text class="info-row__grow">{{ order.address || "-" }}</text>
          <wd-button v-if="canNavigate" size="small" plain @click.stop="onNavigate">导航</wd-button>
        </view>
        <view class="info-row">
          <wd-icon name="clock" size="30rpx" color="#86909c" />
          <text>{{ order.appointDate || "-" }} {{ order.appointPeriod || "" }}</text>
        </view>
        <view v-if="order.remark" class="info-row">
          <wd-icon name="chat" size="30rpx" color="#86909c" />
          <text>{{ order.remark }}</text>
        </view>
      </view>

      <view class="card">
        <view class="card__title">回收明细</view>
        <view v-if="lineItems.length">
          <view v-for="(it, i) in lineItems" :key="i" class="item-row">
            <text class="item-row__name">{{ it.skuName }}</text>
            <text class="item-row__meta">{{ it.meta }}</text>
            <text class="item-row__amount">{{ it.amount ? "¥" + it.amount : "-" }}</text>
          </view>
        </view>
        <view v-else class="card__empty">暂无明细</view>
        <view class="amount-row">
          <text>预估金额</text>
          <text>¥{{ order.estimateAmount || "0.00" }}</text>
        </view>
        <view class="amount-row amount-row--main">
          <text>实收金额</text>
          <text class="amount-row__money">¥{{ order.actualAmount || "0.00" }}</text>
        </view>
      </view>

      <view v-if="photos.length" class="card">
        <view class="card__title">现场照片</view>
        <view class="photo-wall">
          <image
            v-for="(img, i) in photos"
            :key="i"
            :src="img"
            mode="aspectFill"
            class="photo-wall__img"
            @tap="previewPhoto(i)"
          />
        </view>
      </view>

      <wd-button
        v-if="order.status === 'ACCEPTED'"
        type="primary"
        block
        custom-class="bdetail__action"
        @click="onStart"
      >
        开始服务
      </wd-button>
      <wd-button
        v-if="order.status === 'SERVING' || order.status === 'WEIGHED'"
        type="primary"
        block
        custom-class="bdetail__action"
        @click="goHandle"
      >
        {{ order.status === "SERVING" ? "称重录入" : "收款完成" }}
      </wd-button>
    </view>
    <view v-else class="bdetail__loading"><wd-loading color="#07c160" /></view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from "vue";
import { onLoad, onShow } from "@dcloudio/uni-app";
import { getBossOrderDetail, startService } from "@/api/boss";
import { orderLineItems, type OrderVO } from "@/api/order";
import { statusText, statusType } from "@/utils/order-status";
import { openNavigation } from "@/utils/map-nav";

const order = ref<OrderVO | null>(null);
let orderId = "";
let loadedOnce = false;

const canNavigate = computed(() => {
  const lat = Number(order.value?.latitude);
  const lng = Number(order.value?.longitude);
  return !!lat && !!lng && !Number.isNaN(lat) && !Number.isNaN(lng);
});

function onNavigate() {
  if (!canNavigate.value || !order.value) return;
  openNavigation({
    latitude: Number(order.value.latitude),
    longitude: Number(order.value.longitude),
    name: order.value.receiver ? `${order.value.receiver}的取件地址` : "取件地址",
    address: order.value.address || "",
  });
}

/** 已称重取实收明细，否则展示预估明细并标注 */
const lineItems = computed(() => {
  const o = order.value;
  if (!o) return [];
  const hasActual = !!o.actualItems?.length;
  const items = hasActual ? o.actualItems! : orderLineItems(o);
  return items.map((it) => {
    const weight = it.weight || it.estimateWeight;
    return {
      skuName: it.skuName || it.skuId,
      meta: weight ? (hasActual ? `${weight}kg` : `预估 ${weight}kg`) : "-",
      amount: it.amount,
    };
  });
});

/** 用户下单照片 + 称重现场照片 */
const photos = computed<string[]>(() => [
  ...(order.value?.images || []),
  ...(order.value?.weighImages || []),
]);

function previewPhoto(i: number) {
  uni.previewImage({ urls: photos.value, current: i });
}

async function load() {
  try {
    order.value = await getBossOrderDetail(orderId);
  } catch (e) {
    /* 错误提示已由 request 统一处理 */
  }
}

async function onStart() {
  try {
    await startService(orderId);
    uni.showToast({ title: "已开始服务", icon: "success" });
    load();
  } catch (e) {
    /* 错误提示已由 request 统一处理 */
  }
}

function goHandle() {
  uni.navigateTo({ url: `/pages-recycler/order/handle?id=${orderId}` });
}

onLoad((options) => {
  orderId = options?.id || "";
  if (orderId) {
    load();
    loadedOnce = true;
  }
});

onShow(() => {
  // 从称重页返回时刷新状态
  if (loadedOnce && orderId) load();
});
</script>

<style lang="scss" scoped>
.bdetail {
  padding: 24rpx 32rpx 48rpx;

  &__loading {
    display: flex;
    justify-content: center;
    padding: 120rpx 0;
  }

  :deep(.bdetail__action) {
    margin-top: 12rpx;
    border-radius: 44rpx !important;
  }
}

.status-head {
  display: flex;
  align-items: center;
  gap: 20rpx;
  background: #fff;
  border-radius: 24rpx;
  padding: 28rpx;
  margin-bottom: 24rpx;

  &__no {
    font-size: 26rpx;
    color: #4e5969;
  }
}

.card {
  background: #fff;
  border-radius: 24rpx;
  padding: 28rpx;
  margin-bottom: 24rpx;

  &__title {
    font-size: 30rpx;
    font-weight: 700;
    margin-bottom: 20rpx;
  }

  &__empty {
    color: #c0c4cc;
    font-size: 26rpx;
    padding: 12rpx 0 24rpx;
  }
}

.info-row {
  display: flex;
  align-items: center;
  gap: 14rpx;
  padding: 10rpx 0;
  font-size: 27rpx;
  color: #4e5969;

  &__grow {
    flex: 1;
    min-width: 0;
  }
}

.photo-wall {
  display: flex;
  flex-wrap: wrap;
  gap: 16rpx;

  &__img {
    width: 156rpx;
    height: 156rpx;
    border-radius: 12rpx;
  }
}

.item-row {
  display: flex;
  align-items: center;
  padding: 16rpx 0;
  font-size: 26rpx;
  border-bottom: 1rpx solid #f7f8fa;

  &__name {
    flex: 1.5;
  }

  &__meta {
    flex: 1;
    color: #86909c;
  }

  &__amount {
    flex: 1;
    text-align: right;
  }
}

.amount-row {
  display: flex;
  justify-content: space-between;
  padding-top: 20rpx;
  font-size: 26rpx;
  color: #86909c;

  &--main {
    color: #1f2329;
    font-weight: 600;
  }

  &__money {
    color: #ff4d4f;
    font-size: 34rpx;
    font-weight: 700;
  }
}
</style>
