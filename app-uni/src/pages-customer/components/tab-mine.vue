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
      <!-- 环保成就 -->
      <view class="eco-card">
        <view class="eco-card__head">
          <wd-icon name="discount" size="34rpx" color="#07c160" />
          <text>环保成就</text>
        </view>
        <view class="eco-card__grid">
          <view class="eco-card__item">
            <view class="eco-card__value">{{ stats.totalWeightKg ?? "0" }}<text class="eco-card__unit">kg</text></view>
            <view class="eco-card__label">累计回收</view>
          </view>
          <view class="eco-card__divider" />
          <view class="eco-card__item">
            <view class="eco-card__value">{{ stats.completedOrders ?? 0 }}<text class="eco-card__unit">单</text></view>
            <view class="eco-card__label">完成订单</view>
          </view>
          <view class="eco-card__divider" />
          <view class="eco-card__item">
            <view class="eco-card__value">{{ stats.co2SavedKg ?? "0" }}<text class="eco-card__unit">kg</text></view>
            <view class="eco-card__label">累计减碳</view>
          </view>
        </view>
      </view>

      <view class="menu-card">
        <view class="menu-item" @tap="goWallet">
          <view class="menu-item__left">
            <view class="menu-item__icon" style="background: #f3ebfe">
              <wd-icon name="money-circle" size="36rpx" color="#9c6ade" />
            </view>
            <text>我的钱包</text>
          </view>
          <wd-icon name="arrow-right" size="30rpx" color="#c0c4cc" />
        </view>
        <view class="menu-item" @tap="goNotice">
          <view class="menu-item__left">
            <wd-badge :model-value="unreadCount" :max="99">
              <view class="menu-item__icon" style="background: #fff8e0">
                <wd-icon name="notification" size="36rpx" color="#f5a623" />
              </view>
            </wd-badge>
            <text>消息通知</text>
          </view>
          <wd-icon name="arrow-right" size="30rpx" color="#c0c4cc" />
        </view>
        <view class="menu-item" @tap="goProfile">
          <view class="menu-item__left">
            <view class="menu-item__icon" style="background: #e8f9ef">
              <wd-icon name="user" size="36rpx" color="#07c160" />
            </view>
            <text>个人资料</text>
          </view>
          <wd-icon name="arrow-right" size="30rpx" color="#c0c4cc" />
        </view>
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
import { computed, ref } from "vue";
import { useUserStore } from "@/store/user";
import { logout as apiLogout } from "@/api/auth";
import { getUnreadNoticeCount, getUserStats, type UserStats } from "@/api/user";

const userStore = useUserStore();

const unreadCount = ref(0);
const stats = ref<UserStats>({});

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
  loadUnread();
  loadStats();
}

async function loadUnread() {
  try {
    unreadCount.value = Number(await getUnreadNoticeCount()) || 0;
  } catch (e) {
    /* 接口未就绪时不展示角标 */
  }
}

async function loadStats() {
  try {
    stats.value = (await getUserStats()) || {};
  } catch (e) {
    /* 接口未就绪时展示 0 */
  }
}

function goWallet() {
  uni.navigateTo({ url: "/pages-customer/wallet/index" });
}

function goNotice() {
  uni.navigateTo({ url: "/pages-customer/notice/index" });
}

function goProfile() {
  uni.navigateTo({ url: "/pages-customer/profile/index" });
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

.eco-card {
  background: linear-gradient(160deg, #e8f9ef 0%, #ffffff 55%);
  border: 1rpx solid rgba(7, 193, 96, 0.18);
  border-radius: 24rpx;
  padding: 26rpx 28rpx;
  margin-bottom: 24rpx;

  &__head {
    display: flex;
    align-items: center;
    gap: 10rpx;
    font-size: 28rpx;
    font-weight: 700;
    color: #1f2329;
  }

  &__grid {
    margin-top: 24rpx;
    display: flex;
    align-items: center;
  }

  &__item {
    flex: 1;
    text-align: center;
  }

  &__value {
    font-size: 36rpx;
    font-weight: 700;
    color: $theme-color;
  }

  &__unit {
    margin-left: 4rpx;
    font-size: 22rpx;
    font-weight: 400;
    color: #86909c;
  }

  &__label {
    margin-top: 8rpx;
    font-size: 22rpx;
    color: #86909c;
  }

  &__divider {
    width: 1rpx;
    height: 48rpx;
    background: rgba(7, 193, 96, 0.15);
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
