<template>
  <view class="result">
    <view v-if="loading" class="result__loading"><wd-loading color="#07c160" /></view>

    <template v-else>
      <view class="result__icon" :class="`result__icon--${status}`">
        <wd-icon :name="statusIcon" size="88rpx" color="#ffffff" />
      </view>
      <view class="result__title">{{ statusTitle }}</view>
      <view class="result__desc">{{ statusDesc }}</view>

      <view v-if="apply" class="card">
        <view class="card__row">
          <text class="card__label">门店名称</text>
          <text>{{ apply.storeName }}</text>
        </view>
        <view class="card__row">
          <text class="card__label">联系人</text>
          <text>{{ apply.contactName }} {{ apply.contactPhone }}</text>
        </view>
        <view class="card__row">
          <text class="card__label">门店地址</text>
          <text>{{ (apply.province || "") + (apply.city || "") + (apply.district || "") + apply.detail }}</text>
        </view>
        <view v-if="status === 'rejected' && apply.auditRemark" class="card__row card__row--reject">
          <text class="card__label">驳回原因</text>
          <text>{{ apply.auditRemark }}</text>
        </view>
      </view>

      <wd-button
        v-if="status === 'approved'"
        type="primary"
        block
        size="large"
        custom-class="result__btn"
        @click="onRelogin"
      >
        重新登录，进入回收商工作台
      </wd-button>
      <wd-button
        v-else-if="status === 'rejected'"
        type="primary"
        block
        size="large"
        custom-class="result__btn"
        @click="onReapply"
      >
        重新提交申请
      </wd-button>
      <wd-button v-else plain block size="large" custom-class="result__btn" @click="onBack">
        返回
      </wd-button>
    </template>
  </view>
</template>

<script setup lang="ts">
import { ref, computed } from "vue";
import { onLoad } from "@dcloudio/uni-app";
import { getLatestApply } from "@/api/boss";
import type { ApplyVO } from "@/api/boss";
import { useUserStore } from "@/store/user";
import { logout as apiLogout } from "@/api/auth";

const userStore = useUserStore();
const apply = ref<ApplyVO | null>(null);
const loading = ref(true);

const status = computed(() => apply.value?.auditStatus || "pending");

const statusIcon = computed(() => {
  switch (status.value) {
    case "approved":
      return "check-bold";
    case "rejected":
      return "close-bold";
    default:
      return "hourglass";
  }
});

const statusTitle = computed(() => {
  switch (status.value) {
    case "approved":
      return "入驻审核已通过";
    case "rejected":
      return "申请被驳回";
    default:
      return "审核中";
  }
});

const statusDesc = computed(() => {
  switch (status.value) {
    case "approved":
      return "您的账号已升级为回收商，重新登录后即可进入工作台接单";
    case "rejected":
      return "请根据驳回原因修改后重新提交";
    default:
      return "平台将在 1-3 个工作日内完成审核，请耐心等待";
  }
});

async function load() {
  loading.value = true;
  try {
    apply.value = await getLatestApply();
    if (!apply.value) {
      uni.redirectTo({ url: "/pages-recycler/apply/index" });
      return;
    }
    // 同步本地审核状态，供路由拦截使用
    userStore.setRecyclerStatus(apply.value.auditStatus);
  } catch (e) {
    /* 保持 pending 展示 */
  } finally {
    loading.value = false;
  }
}

async function onRelogin() {
  try {
    await apiLogout();
  } catch (e) {
    /* 忽略登出接口失败 */
  }
  userStore.logout();
  uni.reLaunch({ url: "/pages/login/index" });
}

function onReapply() {
  uni.redirectTo({ url: "/pages-recycler/apply/index" });
}

function onBack() {
  const pages = getCurrentPages();
  if (pages.length > 1) {
    uni.navigateBack();
  } else {
    uni.reLaunch({ url: userStore.token ? "/pages-customer/index" : "/pages/login/index" });
  }
}

onLoad(() => {
  load();
});
</script>

<style lang="scss" scoped>
.result {
  padding: 96rpx 48rpx;
  display: flex;
  flex-direction: column;
  align-items: center;

  &__loading {
    padding: 120rpx 0;
  }

  &__icon {
    width: 160rpx;
    height: 160rpx;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    background: linear-gradient(135deg, #ff8f1f, #ffab4a);

    &--approved {
      background: $theme-gradient;
    }

    &--rejected {
      background: linear-gradient(135deg, #f53f3f, #ff7d7d);
    }
  }

  &__title {
    margin-top: 40rpx;
    font-size: 40rpx;
    font-weight: 700;
  }

  &__desc {
    margin-top: 16rpx;
    font-size: 26rpx;
    color: #86909c;
    text-align: center;
    line-height: 1.6;
  }

  :deep(.result__btn) {
    margin-top: 64rpx;
    border-radius: 48rpx !important;
    width: 100%;
  }
}

.card {
  margin-top: 48rpx;
  width: 100%;
  box-sizing: border-box;
  background: #fff;
  border-radius: 24rpx;
  padding: 12rpx 28rpx;

  &__row {
    display: flex;
    gap: 24rpx;
    padding: 22rpx 0;
    font-size: 27rpx;
    border-bottom: 1rpx solid #f7f8fa;

    &:last-child {
      border-bottom: none;
    }

    &--reject {
      color: #f53f3f;
    }
  }

  &__label {
    color: #86909c;
    width: 140rpx;
    flex-shrink: 0;
  }
}
</style>
