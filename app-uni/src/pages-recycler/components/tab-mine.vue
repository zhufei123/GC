<template>
  <view class="bmine">
    <view class="bmine__header">
      <view class="bmine__avatar">
        <wd-icon name="shop" size="64rpx" color="#07c160" />
      </view>
      <view class="bmine__info">
        <view class="bmine__name">{{ store.name || userStore.nickname || "我的回收站" }}</view>
        <view class="bmine__badge">
          <wd-tag type="success">已入驻</wd-tag>
          <wd-tag :type="businessOn ? 'primary' : 'default'" plain>
            {{ businessOn ? "营业中" : "休息中" }}
          </wd-tag>
        </view>
      </view>
    </view>

    <view class="bmine__body">
      <view class="store-card">
        <view class="store-card__title">门店信息</view>
        <view class="store-row">
          <text class="store-row__label">联系人</text>
          <text class="store-row__value">{{ store.contactName || "-" }}</text>
        </view>
        <view class="store-row">
          <text class="store-row__label">联系电话</text>
          <text class="store-row__value">{{ store.phone || "-" }}</text>
        </view>
        <view class="store-row">
          <text class="store-row__label">营业时间</text>
          <view v-if="!hoursEditing" class="store-row__editable" @tap="startEditHours">
            <text class="store-row__value">{{ store.businessHours || "09:00-18:00" }}</text>
            <wd-icon name="edit-outline" size="30rpx" color="#86909c" />
          </view>
          <view v-else class="hours-edit">
            <input
              class="hours-edit__input"
              :value="hoursInput"
              placeholder="如 09:00-18:00"
              :maxlength="20"
              @input="(e: any) => (hoursInput = e.detail.value)"
            />
            <wd-button size="small" type="primary" :loading="hoursSaving" @click="saveHours">
              保存
            </wd-button>
            <wd-button size="small" plain :disabled="hoursSaving" @click="hoursEditing = false">
              取消
            </wd-button>
          </view>
        </view>
        <view class="store-row">
          <text class="store-row__label">门店地址</text>
          <text class="store-row__value">
            {{ (store.province || "") + (store.city || "") + (store.district || "") + (store.address || "-") }}
          </text>
        </view>
        <view class="store-row store-row--last">
          <text class="store-row__label">营业状态</text>
          <wd-switch :model-value="businessOn" size="44rpx" @change="onToggleBusiness" />
        </view>
      </view>

      <view class="menu-card" @tap="goPrice">
        <view class="menu-card__left">
          <view class="menu-card__icon">
            <wd-icon name="money-circle" size="36rpx" color="#07c160" />
          </view>
          <text class="menu-card__title">我的报价</text>
        </view>
        <view class="menu-card__right">
          <text class="menu-card__desc">发布本站回收价</text>
          <wd-icon name="arrow-right" size="28rpx" color="#c0c4cc" />
        </view>
      </view>

      <wd-button plain block custom-class="bmine__logout" @click="onLogout">退出登录</wd-button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref, reactive } from "vue";
import { getBossStore, updateBusinessStatus, updateBusinessHours } from "@/api/boss";
import { logout as apiLogout } from "@/api/auth";
import { useUserStore } from "@/store/user";

const userStore = useUserStore();
const store = reactive<Record<string, any>>({});
const businessOn = ref(true);
const hoursEditing = ref(false);
const hoursInput = ref("");
const hoursSaving = ref(false);

async function refresh() {
  try {
    const res = await getBossStore();
    Object.assign(store, res || {});
    if (typeof res?.businessStatus !== "undefined") {
      businessOn.value = Number(res.businessStatus) === 1;
    }
  } catch (e) {
    /* 保持现有数据 */
  }
}

function startEditHours() {
  hoursInput.value = store.businessHours || "09:00-18:00";
  hoursEditing.value = true;
}

async function saveHours() {
  const value = hoursInput.value.trim();
  if (!value) {
    uni.showToast({ title: "请填写营业时间", icon: "none" });
    return;
  }
  // 宽校验：HH:mm-HH:mm，与 C 端 openNow 解析规则一致
  if (!/^([01]\d|2[0-3]):[0-5]\d\s*-\s*([01]\d|2[0-3]):[0-5]\d$/.test(value)) {
    uni.showToast({ title: "格式应为 09:00-18:00", icon: "none" });
    return;
  }
  hoursSaving.value = true;
  try {
    await updateBusinessHours(value);
    store.businessHours = value;
    hoursEditing.value = false;
    uni.showToast({ title: "营业时间已更新", icon: "success" });
  } catch (e) {
    /* 错误提示已由 request 统一处理 */
  } finally {
    hoursSaving.value = false;
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

function goPrice() {
  uni.navigateTo({ url: "/pages-recycler/price/index" });
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

refresh();
defineExpose({ refresh });
</script>

<style lang="scss" scoped>
.bmine {
  &__header {
    background: $theme-gradient;
    padding: 72rpx 40rpx 96rpx;
    display: flex;
    align-items: center;
    gap: 28rpx;
  }

  &__avatar {
    width: 120rpx;
    height: 120rpx;
    border-radius: 32rpx;
    background: #fff;
    display: flex;
    align-items: center;
    justify-content: center;
    box-shadow: 0 8rpx 24rpx rgba(0, 0, 0, 0.1);
  }

  &__name {
    color: #fff;
    font-size: 36rpx;
    font-weight: 700;
  }

  &__badge {
    margin-top: 14rpx;
    display: flex;
    gap: 14rpx;
  }

  &__body {
    margin-top: -48rpx;
    padding: 0 32rpx;
  }

  :deep(.bmine__logout) {
    margin-top: 48rpx;
    border-radius: 48rpx !important;
  }
}

.store-card {
  background: #fff;
  border-radius: 24rpx;
  padding: 28rpx;

  &__title {
    font-size: 30rpx;
    font-weight: 700;
    margin-bottom: 12rpx;
  }
}

.menu-card {
  margin-top: 24rpx;
  background: #fff;
  border-radius: 24rpx;
  padding: 28rpx;
  display: flex;
  align-items: center;
  justify-content: space-between;

  &__left {
    display: flex;
    align-items: center;
    gap: 20rpx;
  }

  &__icon {
    width: 72rpx;
    height: 72rpx;
    border-radius: 20rpx;
    background: $theme-color-light;
    display: flex;
    align-items: center;
    justify-content: center;
  }

  &__title {
    font-size: 29rpx;
    font-weight: 600;
  }

  &__right {
    display: flex;
    align-items: center;
    gap: 8rpx;
  }

  &__desc {
    font-size: 24rpx;
    color: #86909c;
  }
}

.store-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  min-height: 88rpx;
  border-bottom: 1rpx solid #f2f3f5;

  &--last {
    border-bottom: none;
  }

  &__label {
    font-size: 27rpx;
    color: #86909c;
    flex-shrink: 0;
  }

  &__value {
    font-size: 27rpx;
    color: #1f2329;
    text-align: right;
    margin-left: 32rpx;
  }

  &__editable {
    display: flex;
    align-items: center;
    gap: 12rpx;
  }
}

.hours-edit {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 16rpx;
  margin-left: 32rpx;

  &__input {
    flex: 1;
    max-width: 260rpx;
    font-size: 26rpx;
    background: #f7f8fa;
    border-radius: 12rpx;
    padding: 12rpx 16rpx;
    text-align: center;
  }
}
</style>
