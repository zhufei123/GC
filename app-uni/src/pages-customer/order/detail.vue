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

      <!-- 打款信息（C2B：回收站付客户） -->
      <view v-if="order.status === 'COMPLETED' && order.payMethod" class="card">
        <view class="card__title">打款信息</view>
        <view class="pay-row">
          <text class="pay-row__label">打款方式</text>
          <text>{{ payMethodText(order.payMethod) }}</text>
        </view>
        <view class="pay-row">
          <text class="pay-row__label">打款状态</text>
          <wd-tag :type="order.payoutStatus === 'SUCCESS' ? 'success' : 'warning'" plain>
            {{ payoutStatusText(order.payoutStatus) }}
          </wd-tag>
        </view>
        <view v-if="order.paidAt" class="pay-row">
          <text class="pay-row__label">打款时间</text>
          <text>{{ order.paidAt }}</text>
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

      <view v-if="order.remark" class="card">
        <view class="card__title">备注</view>
        <view class="card__text">{{ order.remark }}</view>
      </view>

      <view v-if="order.cancelReason" class="card">
        <view class="card__title">取消原因</view>
        <view class="card__text">{{ order.cancelReason }}</view>
      </view>

      <!-- 订单评价：完成后可评，一单一评 -->
      <view v-if="order.status === 'COMPLETED'" class="card">
        <view class="card__title">服务评价</view>

        <template v-if="review">
          <view class="review__done">
            <wd-rate :model-value="review.rating" readonly size="18px" active-color="#ff8f1f" />
            <text class="review__done-score">{{ review.rating }}.0 分</text>
          </view>
          <view v-if="review.comment" class="review__comment">{{ review.comment }}</view>
          <view v-if="review.createTime" class="review__time">评价于 {{ review.createTime }}</view>
        </template>

        <template v-else-if="reviewLoaded">
          <view class="review__rate-row">
            <wd-rate v-model="ratingInput" size="22px" active-color="#ff8f1f" />
            <text class="review__rate-text">{{ RATING_TEXTS[ratingInput] || "点击星星评分" }}</text>
          </view>
          <wd-textarea
            v-model="commentInput"
            placeholder="说说本次回收体验吧（选填）"
            :maxlength="500"
            show-word-limit
            custom-class="review__textarea"
          />
          <wd-button type="primary" block custom-class="review__submit" @click="onSubmitReview">
            去评价
          </wd-button>
        </template>

        <view v-else class="review__loading"><wd-loading color="#07c160" /></view>
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
import { getOrderDetail, cancelOrder, getOrderReview, submitOrderReview } from "@/api/order";
import type { OrderVO, OrderReviewVO } from "@/api/order";
import { statusText, payMethodText, payoutStatusText } from "@/utils/order-status";

const RATING_TEXTS: Record<number, string> = {
  1: "很不满意",
  2: "不满意",
  3: "一般",
  4: "满意",
  5: "非常满意",
};

const order = ref<OrderVO | null>(null);
const review = ref<OrderReviewVO | null>(null);
const reviewLoaded = ref(false);
const ratingInput = ref(5);
const commentInput = ref("");
let orderId = "";
let submittingReview = false;

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

/** 下单照片 + 称重现场照片 */
const photos = computed<string[]>(() => [
  ...(order.value?.images || []),
  ...(order.value?.weighImages || []),
]);

function previewPhoto(i: number) {
  uni.previewImage({ urls: photos.value, current: i });
}

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
      return "已称重，等待回收站付款完成";
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
    if (order.value?.status === "COMPLETED") {
      loadReview();
    }
  } catch (e) {
    /* 错误提示已由 request 统一处理 */
  }
}

async function loadReview() {
  try {
    review.value = (await getOrderReview(orderId)) || null;
  } catch (e) {
    review.value = null;
  } finally {
    reviewLoaded.value = true;
  }
}

async function onSubmitReview() {
  if (submittingReview) return;
  if (!ratingInput.value || ratingInput.value < 1) {
    uni.showToast({ title: "请先选择评分", icon: "none" });
    return;
  }
  submittingReview = true;
  try {
    await submitOrderReview(orderId, ratingInput.value, commentInput.value.trim() || undefined);
    uni.showToast({ title: "感谢您的评价", icon: "success" });
    loadReview();
  } catch (e) {
    /* 错误提示已由 request 统一处理 */
  } finally {
    submittingReview = false;
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

.pay-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12rpx 0;
  font-size: 27rpx;
  color: #1f2329;

  &__label {
    color: #86909c;
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

.review {
  &__done {
    display: flex;
    align-items: center;
    gap: 16rpx;
  }

  &__done-score {
    color: #ff8f1f;
    font-size: 28rpx;
    font-weight: 700;
  }

  &__comment {
    margin-top: 16rpx;
    font-size: 26rpx;
    color: #4e5969;
    background: #f7f8fa;
    border-radius: 16rpx;
    padding: 20rpx;
  }

  &__time {
    margin-top: 12rpx;
    font-size: 22rpx;
    color: #c0c4cc;
  }

  &__rate-row {
    display: flex;
    align-items: center;
    gap: 20rpx;
    padding: 8rpx 0 20rpx;
  }

  &__rate-text {
    font-size: 26rpx;
    color: #86909c;
  }

  &__loading {
    display: flex;
    justify-content: center;
    padding: 24rpx 0;
  }
}

:deep(.review__textarea) {
  background: #f7f8fa;
  border-radius: 16rpx;
}

:deep(.review__submit) {
  margin-top: 24rpx;
  border-radius: 44rpx !important;
}
</style>
