<template>
  <view class="addr-edit">
    <view class="card">
      <view class="form-row">
        <text class="form-row__label">联系人</text>
        <input v-model="form.receiver" class="form-row__input" placeholder="收货人姓名" />
      </view>
      <view class="form-row">
        <text class="form-row__label">手机号</text>
        <input
          v-model="form.phone"
          class="form-row__input"
          type="number"
          :maxlength="11"
          placeholder="11位手机号"
        />
      </view>
      <view class="form-row">
        <text class="form-row__label">省市区</text>
        <view class="form-row__region">
          <input v-model="form.province" class="form-row__region-input" placeholder="省" />
          <input v-model="form.city" class="form-row__region-input" placeholder="市" />
          <input v-model="form.district" class="form-row__region-input" placeholder="区" />
        </view>
      </view>
      <view class="form-row">
        <text class="form-row__label">街道</text>
        <input v-model="form.street" class="form-row__input" placeholder="街道 / 路名（选填）" />
      </view>
      <view class="form-row">
        <text class="form-row__label">详细地址</text>
        <input v-model="form.detail" class="form-row__input" placeholder="小区、楼栋、门牌号" />
      </view>
      <view class="form-row form-row--last">
        <text class="form-row__label">设为默认</text>
        <wd-switch v-model="form.isDefault" size="44rpx" />
      </view>
    </view>

    <wd-button type="primary" block :loading="saving" custom-class="addr-edit__save" @click="onSave">
      保存
    </wd-button>
    <wd-button
      v-if="form.id"
      plain
      block
      custom-class="addr-edit__delete"
      @click="onDelete"
    >
      删除地址
    </wd-button>
  </view>
</template>

<script setup lang="ts">
import { reactive, ref } from "vue";
import { onLoad } from "@dcloudio/uni-app";
import { createAddress, updateAddress, deleteAddress } from "@/api/address";
import type { AddressItem } from "@/api/address";

const form = reactive<AddressItem>({
  receiver: "",
  phone: "",
  province: "广东省",
  city: "深圳市",
  district: "",
  street: "",
  detail: "",
  isDefault: false,
});
const saving = ref(false);

onLoad((options) => {
  if (options?.data) {
    try {
      Object.assign(form, JSON.parse(decodeURIComponent(options.data)));
      // 后端返回 1/0，归一成布尔供 wd-switch 使用
      form.isDefault = form.isDefault === true || Number(form.isDefault) === 1;
      uni.setNavigationBarTitle({ title: "编辑地址" });
    } catch (e) {
      /* 忽略非法参数 */
    }
  } else {
    uni.setNavigationBarTitle({ title: "新增地址" });
  }
});

async function onSave() {
  if (!form.receiver) return uni.showToast({ title: "请填写联系人", icon: "none" });
  if (!/^1\d{10}$/.test(form.phone)) return uni.showToast({ title: "手机号格式不正确", icon: "none" });
  if (!form.province || !form.city || !form.district)
    return uni.showToast({ title: "请填写省市区", icon: "none" });
  if (!form.detail) return uni.showToast({ title: "请填写详细地址", icon: "none" });

  saving.value = true;
  // 只提交 AddressDTO 需要的字段，避免把实体多余字段(id/userId/createTime)回传
  const payload: AddressItem = {
    receiver: form.receiver,
    phone: form.phone,
    province: form.province,
    city: form.city,
    district: form.district,
    street: form.street,
    detail: form.detail,
    longitude: form.longitude,
    latitude: form.latitude,
    isDefault: !!form.isDefault,
  };
  try {
    if (form.id) {
      await updateAddress(String(form.id), payload);
    } else {
      await createAddress(payload);
    }
    uni.showToast({ title: "已保存", icon: "success" });
    setTimeout(() => uni.navigateBack(), 600);
  } catch (e) {
    /* 错误提示已由 request 统一处理 */
  } finally {
    saving.value = false;
  }
}

function onDelete() {
  uni.showModal({
    title: "删除地址",
    content: "确定删除该地址吗？",
    success: async (res) => {
      if (!res.confirm) return;
      try {
        await deleteAddress(String(form.id));
        uni.showToast({ title: "已删除", icon: "success" });
        setTimeout(() => uni.navigateBack(), 600);
      } catch (e) {
        /* 错误提示已由 request 统一处理 */
      }
    },
  });
}
</script>

<style lang="scss" scoped>
.addr-edit {
  padding: 24rpx 32rpx;

  :deep(.addr-edit__save) {
    margin-top: 48rpx;
    border-radius: 44rpx !important;
  }

  :deep(.addr-edit__delete) {
    margin-top: 24rpx;
    border-radius: 44rpx !important;
  }
}

.card {
  background: #fff;
  border-radius: 24rpx;
  padding: 8rpx 28rpx;
}

.form-row {
  display: flex;
  align-items: center;
  min-height: 104rpx;
  border-bottom: 1rpx solid #f2f3f5;

  &--last {
    border-bottom: none;
    justify-content: space-between;
  }

  &__label {
    width: 160rpx;
    font-size: 28rpx;
    color: #4e5969;
    flex-shrink: 0;
  }

  &__input {
    flex: 1;
    font-size: 28rpx;
  }

  &__region {
    flex: 1;
    display: flex;
    gap: 16rpx;
  }

  &__region-input {
    flex: 1;
    font-size: 28rpx;
    background: #f7f8fa;
    border-radius: 12rpx;
    padding: 12rpx 16rpx;
  }
}
</style>
