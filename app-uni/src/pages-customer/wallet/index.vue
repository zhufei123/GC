<template>
  <view class="wallet">
    <view class="wallet__card">
      <view class="wallet__label">钱包余额（元）</view>
      <view class="wallet__balance">{{ balance }}</view>
      <view class="wallet__hint">回收订单选择「平台钱包」打款后自动入账</view>
    </view>

    <view class="ledger">
      <view class="ledger__title">最近流水</view>
      <view v-if="list.length">
        <view v-for="item in list" :key="item.id" class="ledger-row">
          <view class="ledger-row__icon" :class="{ 'ledger-row__icon--out': isOut(item) }">
            <wd-icon :name="isOut(item) ? 'arrow-up' : 'arrow-down'" size="30rpx" color="#ffffff" />
          </view>
          <view class="ledger-row__info">
            <view class="ledger-row__remark">{{ item.remark || item.bizType || "钱包变动" }}</view>
            <view class="ledger-row__time">{{ item.createTime || "-" }}</view>
          </view>
          <view class="ledger-row__amount" :class="{ 'ledger-row__amount--out': isOut(item) }">
            {{ isOut(item) ? "" : "+" }}{{ item.amount }}
          </view>
        </view>
      </view>
      <wd-status-tip v-else-if="!loading" image="content" tip="暂无流水记录" />
      <view v-if="loading" class="wallet__loading"><wd-loading color="#07c160" /></view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { onShow } from "@dcloudio/uni-app";
import { getWallet, type WalletLedgerItem } from "@/api/user";

const balance = ref("0.00");
const list = ref<WalletLedgerItem[]>([]);
const loading = ref(true);

function isOut(item: WalletLedgerItem) {
  return parseFloat(item.amount || "0") < 0;
}

async function load() {
  loading.value = true;
  try {
    const data = await getWallet();
    balance.value = data?.balance != null ? String(data.balance) : "0.00";
    list.value = data?.list || [];
  } catch (e) {
    /* 错误提示已由 request 统一处理 */
  } finally {
    loading.value = false;
  }
}

onShow(load);
</script>

<style lang="scss" scoped>
.wallet {
  min-height: 100vh;
  background: $page-bg;
  padding: 24rpx 32rpx 48rpx;

  &__card {
    background: $theme-gradient;
    border-radius: 28rpx;
    padding: 48rpx 40rpx;
  }

  &__label {
    font-size: 26rpx;
    color: rgba(255, 255, 255, 0.85);
  }

  &__balance {
    margin-top: 16rpx;
    font-size: 72rpx;
    font-weight: 700;
    color: #fff;
  }

  &__hint {
    margin-top: 16rpx;
    font-size: 22rpx;
    color: rgba(255, 255, 255, 0.75);
  }

  &__loading {
    display: flex;
    justify-content: center;
    padding: 48rpx 0;
  }
}

.ledger {
  margin-top: 24rpx;
  background: #fff;
  border-radius: 24rpx;
  padding: 28rpx;

  &__title {
    font-size: 30rpx;
    font-weight: 700;
    margin-bottom: 12rpx;
  }
}

.ledger-row {
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 24rpx 0;
  border-bottom: 1rpx solid #f2f3f5;

  &:last-child {
    border-bottom: none;
  }

  &__icon {
    width: 64rpx;
    height: 64rpx;
    border-radius: 50%;
    background: #07c160;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;

    &--out {
      background: #ff8f1f;
    }
  }

  &__info {
    flex: 1;
    min-width: 0;
  }

  &__remark {
    font-size: 27rpx;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__time {
    margin-top: 6rpx;
    font-size: 22rpx;
    color: #86909c;
  }

  &__amount {
    font-size: 32rpx;
    font-weight: 700;
    color: #07c160;

    &--out {
      color: #ff8f1f;
    }
  }
}
</style>
