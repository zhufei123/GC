<template>
  <view class="detail">
    <view v-if="order" class="detail__inner">
      <!-- 状态头 -->
      <view class="status-head" :class="`status-head--${(order.status || '').toLowerCase()}`">
        <wd-icon :name="statusIcon" size="56rpx" color="#ffffff" />
        <view>
          <view class="status-head__text">{{ statusText(order.status) }}</view>
          <view class="status-head__desc">{{ statusDesc }}</view>
        </view>
      </view>

      <!-- 地址 -->
      <view class="card">
        <view class="card__title">上门信息</view>
        <view class="info-row">
          <wd-icon name="user" size="30rpx" color="#86909c" />
          <text>{{ order.receiver || "-" }} {{ order.phone || "" }}</text>
        </view>
        <view class="info-row">
          <wd-icon name="location" size="30rpx" color="#86909c" />
          <text>{{ order.address || "-" }}</text>
        </view>
        <view class="info-row">
          <wd-icon name="clock" size="30rpx" color="#86909c" />
          <text>{{ order.appointDate || "-" }} {{ order.appointPeriod || "" }}</text>
        </view>
      </view>

      <!-- 明细 -->
      <view class="card">
        <view class="card__title">回收明细</view>
        <view v-if="lineItems.length">
          <view class="item-head">
            <text class="item-col item-col--name">品类</text>
            <text class="item-col">预估</text>
            <text class="item-col">实收</text>
            <text class="item-col item-col--right">小计</text>
          </view>
          <view v-for="(it, i) in lineItems" :key="i" class="item-row">
            <text class="item-col item-col--name">{{ it.skuName }}</text>
            <text class="item-col">{{ it.estimateWeight ? it.estimateWeight + "kg" : "-" }}</text>
            <text class="item-col">{{ it.actualWeight ? it.actualWeight + "kg" : "-" }}</text>
            <text class="item-col item-col--right">{{ it.amount ? "¥" + it.amount : "-" }}</text>
          </view>
        </view>
        <view v-else class="card__empty">明细待回收员上门确认</view>
        <view class="amount-row">
          <text>预估金额</text>
          <text>¥{{ order.estimateAmount || "0.00" }}</text>
        </view>
        <view class="amount-row amount-row--main">
          <text>实收金额</text>
          <text class="amount-row__money">¥{{ order.actualAmount || "0.00" }}</text>
        </view>
      </view>

      <view v-if="order.remark" class="card">
        <view class="card__title">备注</view>
        <view class="card__text">{{ order.remark }}</view>
      </view>

      <view v-if="order.cancelReason" class="card">
        <view class="card__title">取消原因</view>
        <view class="card__text">{{ order.cancelReason }}</view>
      </view>

      <wd-button
        v-if="order.status === 'PENDING' || order.status === 'ACCEPTED'"
        plain
        block
        custom-class="detail__cancel"
        @click="onCancel"
      >
        取消订单
      </wd-button>
    </view>
    <view v-else class="detail__loading"><wd-loading color="#07c160" /></view>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from "vue";
import { onLoad } from "@dcloudio/uni-app";
import { getOrderDetail, cancelOrder } from "@/api/order";
import type { OrderVO } from "@/api/order";
import { statusText } from "@/utils/order-status";

const order = ref<OrderVO | null>(null);
let orderId = "";

interface DetailRow {
  skuName: string;
  estimateWeight?: string;
  actualWeight?: string;
  amount?: string;
}

/** 预估/实收按 skuId 合并成行：预估列取 estimateItems，实收列取 actualItems */
const lineItems = computed<DetailRow[]>(() => {
  const o = order.value;
  if (!o) return [];
  const actualBySku = new Map((o.actualItems || []).map((it) => [it.skuId, it]));
  const rows: DetailRow[] = (o.estimateItems || []).map((it) => {
    const actual = actualBySku.get(it.skuId);
    if (actual) actualBySku.delete(it.skuId);
    return {
      skuName: it.skuName || it.skuId,
      estimateWeight: it.weight || it.estimateWeight,
      actualWeight: actual?.weight,
      amount: actual ? actual.amount : it.amount,
    };
  });
  // 称重时新增的品类（无预估）
  actualBySku.forEach((it) => {
    rows.push({ skuName: it.skuName || it.skuId, actualWeight: it.weight, amount: it.amount });
  });
  return rows;
});

const statusIcon = computed(() => {
  switch (order.value?.status) {
    case "PENDING":
      return "hourglass";
    case "ACCEPTED":
    case "SERVING":
      return "service";
    case "WEIGHED":
      return "chart-bar";
    case "COMPLETED":
      return "check-circle";
    case "CANCELLED":
      return "close-circle";
    default:
      return "info-circle";
  }
});

const statusDesc = computed(() => {
  switch (order.value?.status) {
    case "PENDING":
      return "等待附近回收站接单";
    case "ACCEPTED":
      return "回收员将按预约时间上门";
    case "SERVING":
      return "回收员正在上门服务中";
    case "WEIGHED":
      return "已称重，等待线下付款完成";
    case "COMPLETED":
      return "订单已完成，感谢您的环保行动";
    case "CANCELLED":
      return "订单已取消";
    default:
      return "";
  }
});

async function load() {
  try {
    order.value = await getOrderDetail(orderId);
  } catch (e) {
    /* 错误提示已由 request 统一处理 */
  }
}

function onCancel() {
  uni.showModal({
    title: "取消订单",
    editable: true,
    placeholderText: "请填写取消原因",
    success: async (res) => {
      if (!res.confirm) return;
      try {
        await cancelOrder(orderId, (res as any).content || "用户主动取消");
        uni.showToast({ title: "已取消", icon: "success" });
        load();
      } catch (e) {
        /* 错误提示已由 request 统一处理 */
      }
    },
  });
}

onLoad((options) => {
  orderId = options?.id || "";
  if (orderId) load();
});
</script>

<style lang="scss" scoped>
.detail {
  padding: 24rpx 32rpx 48rpx;

  &__loading {
    display: flex;
    justify-content: center;
    padding: 120rpx 0;
  }

  :deep(.detail__cancel) {
    margin-top: 12rpx;
    border-radius: 44rpx !important;
  }
}

.status-head {
  display: flex;
  align-items: center;
  gap: 24rpx;
  background: $theme-gradient;
  border-radius: 24rpx;
  padding: 40rpx 32rpx;
  margin-bottom: 24rpx;

  &--cancelled {
    background: linear-gradient(135deg, #86909c, #a9b0b8);
  }

  &--pending {
    background: linear-gradient(135deg, #ff8f1f, #ffab4a);
  }

  &__text {
    color: #fff;
    font-size: 36rpx;
    font-weight: 700;
  }

  &__desc {
    margin-top: 8rpx;
    color: rgba(255, 255, 255, 0.85);
    font-size: 24rpx;
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

  &__text {
    font-size: 26rpx;
    color: #4e5969;
  }
}

.info-row {
  display: flex;
  align-items: center;
  gap: 14rpx;
  padding: 10rpx 0;
  font-size: 27rpx;
  color: #4e5969;
}

.item-head,
.item-row {
  display: flex;
  padding: 14rpx 0;
  font-size: 25rpx;
}

.item-head {
  color: #86909c;
  border-bottom: 1rpx solid #f2f3f5;
}

.item-col {
  flex: 1;

  &--name {
    flex: 1.6;
  }

  &--right {
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
