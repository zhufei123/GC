<template>
  <scroll-view scroll-y class="wb" :style="{ height: 'calc(100vh - 100rpx)' }">
    <view class="wb__header">
      <view class="wb__store">
        <view class="wb__store-icon">
          <wd-icon name="shop" size="44rpx" color="#07c160" />
        </view>
        <view class="wb__store-info">
          <view class="wb__store-name">{{ data.storeName || userStore.nickname || "我的回收站" }}</view>
          <view class="wb__store-status">{{ businessOn ? "营业中 · 可接单" : "休息中 · 暂停接单" }}</view>
        </view>
        <wd-switch :model-value="businessOn" size="48rpx" @change="onToggleBusiness" />
      </view>
    </view>

    <view class="wb__body">
      <!-- 今日数据 -->
      <view class="stat-card">
        <view class="stat-card__title">今日经营</view>
        <view class="stat-card__grid">
          <view class="stat-card__item">
            <view class="stat-card__value">{{ data.todayOrderCount ?? 0 }}</view>
            <view class="stat-card__label">今日订单</view>
          </view>
          <view class="stat-card__item">
            <view class="stat-card__value">{{ data.todayWeightKg ?? "0" }}<text class="stat-card__unit">kg</text></view>
            <view class="stat-card__label">回收重量</view>
          </view>
          <view class="stat-card__item">
            <view class="stat-card__value stat-card__value--money">¥{{ data.todayAmount ?? "0.00" }}</view>
            <view class="stat-card__label">支出金额</view>
          </view>
        </view>
      </view>

      <!-- 待办 -->
      <view class="todo-row">
        <view class="todo-row__item" @tap="$emit('switch-tab', 1)">
          <wd-badge :model-value="data.poolCount || 0" :max="99">
            <view class="todo-row__icon" style="background: #fff3e5">
              <wd-icon name="notification" size="44rpx" color="#ff8f1f" />
            </view>
          </wd-badge>
          <view class="todo-row__title">待抢订单</view>
          <view class="todo-row__desc">大厅新单</view>
        </view>
        <view class="todo-row__item" @tap="$emit('switch-tab', 2)">
          <wd-badge :model-value="data.pendingCount || 0" :max="99">
            <view class="todo-row__icon" style="background: #e8f0fe">
              <wd-icon name="time" size="44rpx" color="#4d80f0" />
            </view>
          </wd-badge>
          <view class="todo-row__title">待上门</view>
          <view class="todo-row__desc">已接订单</view>
        </view>
        <view class="todo-row__item" @tap="$emit('switch-tab', 2)">
          <wd-badge :model-value="data.servingCount || 0" :max="99">
            <view class="todo-row__icon" style="background: #e8f9ef">
              <wd-icon name="service" size="44rpx" color="#07c160" />
            </view>
          </wd-badge>
          <view class="todo-row__title">服务中</view>
          <view class="todo-row__desc">待称重</view>
        </view>
      </view>

      <!-- 抢单入口 -->
      <view class="hall-banner" @tap="$emit('switch-tab', 1)">
        <view>
          <view class="hall-banner__title">接单大厅</view>
          <view class="hall-banner__desc">附近有新的回收订单，先到先得</view>
        </view>
        <view class="hall-banner__btn">
          去抢单
          <wd-icon name="arrow-right" size="26rpx" color="#ffffff" />
        </view>
      </view>

      <view class="wb__footer">数据实时更新 · 下拉切换 Tab 自动刷新</view>
    </view>
  </scroll-view>
</template>

<script setup lang="ts">
import { ref, reactive } from "vue";
import { getWorkbench, updateBusinessStatus } from "@/api/boss";
import type { WorkbenchData } from "@/api/boss";
import { useUserStore } from "@/store/user";

defineEmits<{ (e: "switch-tab", index: number): void }>();

const userStore = useUserStore();
const data = reactive<WorkbenchData>({});
const businessOn = ref(true);

async function refresh() {
  try {
    const res = await getWorkbench();
    Object.assign(data, res || {});
    if (typeof res?.businessStatus !== "undefined") {
      businessOn.value = Number(res.businessStatus) === 1;
    }
  } catch (e) {
    /* 保持现有数据 */
  }
}

async function onToggleBusiness({ value }: { value: boolean }) {
  const prev = businessOn.value;
  businessOn.value = value;
  try {
    await updateBusinessStatus(value ? 1 : 0);
    uni.showToast({ title: value ? "已开始营业" : "已暂停接单", icon: "none" });
  } catch (e) {
    businessOn.value = prev;
  }
}

refresh();
defineExpose({ refresh });
</script>

<style lang="scss" scoped>
.wb {
  &__header {
    background: $theme-gradient;
    padding: 40rpx 32rpx 100rpx;
  }

  &__store {
    display: flex;
    align-items: center;
    gap: 22rpx;
  }

  &__store-icon {
    width: 88rpx;
    height: 88rpx;
    border-radius: 24rpx;
    background: #fff;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  &__store-info {
    flex: 1;
  }

  &__store-name {
    color: #fff;
    font-size: 34rpx;
    font-weight: 700;
  }

  &__store-status {
    margin-top: 8rpx;
    color: rgba(255, 255, 255, 0.85);
    font-size: 24rpx;
  }

  &__body {
    margin-top: -64rpx;
    padding: 0 32rpx 40rpx;
  }

  &__footer {
    text-align: center;
    color: #c0c4cc;
    font-size: 22rpx;
    padding: 40rpx 0 24rpx;
  }
}

.stat-card {
  background: #fff;
  border-radius: 24rpx;
  padding: 28rpx;

  &__title {
    font-size: 30rpx;
    font-weight: 700;
    margin-bottom: 28rpx;
  }

  &__grid {
    display: flex;
  }

  &__item {
    flex: 1;
    text-align: center;
  }

  &__value {
    font-size: 40rpx;
    font-weight: 700;

    &--money {
      color: #ff4d4f;
    }
  }

  &__unit {
    font-size: 22rpx;
    color: #86909c;
    font-weight: 400;
  }

  &__label {
    margin-top: 10rpx;
    font-size: 24rpx;
    color: #86909c;
  }
}

.todo-row {
  margin-top: 24rpx;
  display: flex;
  gap: 20rpx;

  &__item {
    flex: 1;
    background: #fff;
    border-radius: 24rpx;
    padding: 28rpx 0;
    display: flex;
    flex-direction: column;
    align-items: center;
  }

  &__icon {
    width: 88rpx;
    height: 88rpx;
    border-radius: 24rpx;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  &__title {
    margin-top: 16rpx;
    font-size: 27rpx;
    font-weight: 600;
  }

  &__desc {
    margin-top: 6rpx;
    font-size: 22rpx;
    color: #86909c;
  }
}

.hall-banner {
  margin-top: 24rpx;
  border-radius: 24rpx;
  background: linear-gradient(120deg, #ff8f1f, #ffab4a);
  padding: 36rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 12rpx 32rpx rgba(255, 143, 31, 0.25);

  &__title {
    color: #fff;
    font-size: 34rpx;
    font-weight: 700;
  }

  &__desc {
    margin-top: 10rpx;
    color: rgba(255, 255, 255, 0.9);
    font-size: 24rpx;
  }

  &__btn {
    display: inline-flex;
    align-items: center;
    gap: 6rpx;
    background: rgba(255, 255, 255, 0.25);
    color: #fff;
    font-size: 26rpx;
    font-weight: 600;
    padding: 14rpx 28rpx;
    border-radius: 32rpx;
  }
}
</style>
