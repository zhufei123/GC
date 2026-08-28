<template>
  <view class="mine">
    <view class="mine__header">
      <view class="mine__avatar">
        <wd-icon name="user-avatar" size="72rpx" color="#07c160" />
      </view>
      <view class="mine__info">
        <view class="mine__name">{{ userStore.nickname || "回收用户" }}</view>
        <view class="mine__id">ID: {{ userStore.userId || "-" }}</view>
      </view>
    </view>

    <view class="mine__body">
      <view class="menu-card">
        <view class="menu-item" @tap="goAddress">
          <view class="menu-item__left">
            <view class="menu-item__icon" style="background: #e8f9ef">
              <wd-icon name="location" size="36rpx" color="#07c160" />
            </view>
            <text>地址管理</text>
          </view>
          <wd-icon name="arrow-right" size="30rpx" color="#c0c4cc" />
        </view>
        <view class="menu-item" @tap="goFavorites">
          <view class="menu-item__left">
            <view class="menu-item__icon" style="background: #fdeaf1">
              <wd-icon name="heart-filled" size="36rpx" color="#f0508a" />
            </view>
            <text>我的收藏</text>
          </view>
          <wd-icon name="arrow-right" size="30rpx" color="#c0c4cc" />
        </view>
        <view class="menu-item" @tap="goHelp">
          <view class="menu-item__left">
            <view class="menu-item__icon" style="background: #e0f7fa">
              <wd-icon name="help-circle" size="36rpx" color="#00b4c5" />
            </view>
            <text>帮助中心</text>
          </view>
          <wd-icon name="arrow-right" size="30rpx" color="#c0c4cc" />
        </view>
        <view class="menu-item" @tap="goBoss">
          <view class="menu-item__left">
            <view class="menu-item__icon" style="background: #fff3e5">
              <wd-icon name="shop" size="36rpx" color="#ff8f1f" />
            </view>
            <text>我是回收站老板</text>
          </view>
          <view class="menu-item__right">
            <text v-if="bossHint" class="menu-item__hint">{{ bossHint }}</text>
            <wd-icon name="arrow-right" size="30rpx" color="#c0c4cc" />
          </view>
        </view>
        <view class="menu-item" @tap="onAbout">
          <view class="menu-item__left">
            <view class="menu-item__icon" style="background: #e8f0fe">
              <wd-icon name="info-circle" size="36rpx" color="#4d80f0" />
            </view>
            <text>关于绿色回收</text>
          </view>
          <wd-icon name="arrow-right" size="30rpx" color="#c0c4cc" />
        </view>
      </view>

      <wd-button plain block custom-class="mine__logout" @click="onLogout">退出登录</wd-button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed } from "vue";
import { useUserStore } from "@/store/user";
import { logout as apiLogout } from "@/api/auth";

const userStore = useUserStore();

const bossHint = computed(() => {
  switch (userStore.recyclerStatus) {
    case "pending":
      return "审核中";
    case "rejected":
      return "已驳回";
    case "approved":
      return "已入驻";
    default:
      return "免费入驻";
  }
});

function refresh() {
  /* 静态数据，无需刷新 */
}

function goAddress() {
  uni.navigateTo({ url: "/pages-customer/address/list" });
}

function goFavorites() {
  uni.navigateTo({ url: "/pages-customer/store/favorites" });
}

function goHelp() {
  uni.navigateTo({ url: "/pages-customer/help/index" });
}

function goBoss() {
  if (userStore.recyclerStatus === "approved") {
    uni.showModal({
      title: "提示",
      content: "您已是回收商，请退出后使用「我是回收商」身份重新登录",
      showCancel: false,
    });
    return;
  }
  if (userStore.recyclerStatus === "none") {
    uni.navigateTo({ url: "/pages-recycler/apply/index" });
  } else {
    uni.navigateTo({ url: "/pages-recycler/apply/result" });
  }
}

function onAbout() {
  uni.showModal({
    title: "绿色回收",
    content: "让每一件废品都物尽其用。演示版本 v1.0.0",
    showCancel: false,
  });
}

function onLogout() {
  uni.showModal({
    title: "退出登录",
    content: "确定要退出当前账号吗？",
    success: async (res) => {
      if (!res.confirm) return;
      try {
        await apiLogout();
      } catch (e) {
        /* 忽略登出接口失败 */
      }
      userStore.logout();
      uni.reLaunch({ url: "/pages/login/index" });
    },
  });
}

defineExpose({ refresh });
</script>

<style lang="scss" scoped>
.mine {
  &__header {
    background: $theme-gradient;
    padding: 72rpx 40rpx 96rpx;
    display: flex;
    align-items: center;
    gap: 28rpx;
  }

  &__avatar {
    width: 128rpx;
    height: 128rpx;
    border-radius: 50%;
    background: #fff;
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: 0 8rpx 24rpx rgba(0, 0, 0, 0.1);
  }

  &__name {
    color: #fff;
    font-size: 38rpx;
    font-weight: 700;
  }

  &__id {
    margin-top: 10rpx;
    color: rgba(255, 255, 255, 0.8);
    font-size: 24rpx;
  }

  &__body {
    margin-top: -48rpx;
    padding: 0 32rpx;
  }

  :deep(.mine__logout) {
    margin-top: 48rpx;
    border-radius: 48rpx !important;
  }
}

.menu-card {
  background: #fff;
  border-radius: 24rpx;
  padding: 8rpx 28rpx;
}

.menu-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 30rpx 0;
  border-bottom: 1rpx solid #f2f3f5;

  &:last-child {
    border-bottom: none;
  }

  &__left {
    display: flex;
    align-items: center;
    gap: 22rpx;
    font-size: 30rpx;
  }

  &__icon {
    width: 72rpx;
    height: 72rpx;
    border-radius: 20rpx;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  &__right {
    display: flex;
    align-items: center;
    gap: 10rpx;
  }

  &__hint {
    font-size: 24rpx;
    color: #86909c;
  }
}
</style>
