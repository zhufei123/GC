<template>
  <view class="apply">
    <view class="apply__banner">
      <wd-icon name="shop" size="56rpx" color="#ffffff" />
      <view>
        <view class="apply__banner-title">入驻绿色回收</view>
        <view class="apply__banner-desc">0 费用入驻 · 平台派单 · 当日结算</view>
      </view>
    </view>

    <view class="card">
      <view class="card__title">门店信息</view>
      <view class="form-row">
        <text class="form-row__label">门店名称</text>
        <input v-model="form.storeName" class="form-row__input" placeholder="如：幸福小区回收站" />
      </view>
      <view class="form-row">
        <text class="form-row__label">联系人</text>
        <input v-model="form.contactName" class="form-row__input" placeholder="真实姓名" />
      </view>
      <view class="form-row">
        <text class="form-row__label">联系电话</text>
        <input
          v-model="form.contactPhone"
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
      <view class="form-row form-row--last">
        <text class="form-row__label">详细地址</text>
        <input v-model="form.detail" class="form-row__input" placeholder="街道、门牌号" />
      </view>
    </view>

    <view class="card">
      <view class="card__title">主营品类（选填）</view>
      <view class="chips">
        <view
          v-for="cat in categories"
          :key="cat.id"
          class="chips__item"
          :class="{ 'chips__item--active': selectedCats.includes(cat.id) }"
          @tap="toggleCat(cat.id)"
        >
          {{ cat.name }}
        </view>
        <view v-if="!categories.length" class="chips__empty">分类加载中…</view>
      </view>
    </view>

    <view class="card">
      <view class="card__title">营业执照（选填）</view>
      <view class="upload-box" @tap="onUploadPlaceholder">
        <wd-icon name="camera" size="48rpx" color="#c0c4cc" />
        <text>上传执照照片</text>
      </view>
    </view>

    <wd-button
      type="primary"
      block
      size="large"
      :loading="submitting"
      custom-class="apply__submit"
      @click="onSubmit"
    >
      提交入驻申请
    </wd-button>
  </view>
</template>

<script setup lang="ts">
import { reactive, ref } from "vue";
import { onLoad } from "@dcloudio/uni-app";
import { applyStore, getLatestApply } from "@/api/boss";
import { getCategoryTree } from "@/api/goods";
import { useUserStore } from "@/store/user";

const userStore = useUserStore();

const form = reactive({
  storeName: "",
  contactName: "",
  contactPhone: "",
  province: "广东省",
  city: "深圳市",
  district: "",
  detail: "",
});
const categories = ref<Array<{ id: string; name: string }>>([]);
const selectedCats = ref<string[]>([]);
const submitting = ref(false);

function toggleCat(id: string) {
  const idx = selectedCats.value.indexOf(id);
  if (idx >= 0) selectedCats.value.splice(idx, 1);
  else selectedCats.value.push(id);
}

function onUploadPlaceholder() {
  uni.showToast({ title: "骨架阶段暂不上传，可后补", icon: "none" });
}

async function onSubmit() {
  if (!form.storeName) return uni.showToast({ title: "请填写门店名称", icon: "none" });
  if (!form.contactName) return uni.showToast({ title: "请填写联系人", icon: "none" });
  if (!/^1\d{10}$/.test(form.contactPhone))
    return uni.showToast({ title: "联系电话格式不正确", icon: "none" });
  if (!form.detail) return uni.showToast({ title: "请填写详细地址", icon: "none" });

  submitting.value = true;
  try {
    await applyStore({
      ...form,
      categoryIds: selectedCats.value,
      storeImages: [],
    });
    userStore.setRecyclerStatus("pending");
    uni.showToast({ title: "已提交，等待审核", icon: "success" });
    setTimeout(() => {
      uni.redirectTo({ url: "/pages-recycler/apply/result" });
    }, 700);
  } catch (e) {
    /* 错误提示已由 request 统一处理 */
  } finally {
    submitting.value = false;
  }
}

onLoad(async () => {
  // 已有待审/已通过申请时直接看结果，避免重复提交(60001)
  try {
    const latest = await getLatestApply();
    if (latest && latest.auditStatus !== "rejected") {
      uni.redirectTo({ url: "/pages-recycler/apply/result" });
      return;
    }
  } catch (e) {
    /* 无申请记录时正常展示表单 */
  }
  try {
    const tree = await getCategoryTree();
    categories.value = (tree || []).map((n) => ({ id: n.id, name: n.name }));
  } catch (e) {
    /* 分类为空不阻塞提交 */
  }
});
</script>

<style lang="scss" scoped>
.apply {
  padding: 24rpx 32rpx 64rpx;

  &__banner {
    display: flex;
    align-items: center;
    gap: 24rpx;
    background: $theme-gradient;
    border-radius: 24rpx;
    padding: 36rpx 32rpx;
    margin-bottom: 24rpx;

    &-title {
      color: #fff;
      font-size: 34rpx;
      font-weight: 700;
    }

    &-desc {
      margin-top: 8rpx;
      color: rgba(255, 255, 255, 0.85);
      font-size: 24rpx;
    }
  }

  :deep(.apply__submit) {
    margin-top: 40rpx;
    border-radius: 48rpx !important;
  }
}

.card {
  background: #fff;
  border-radius: 24rpx;
  padding: 20rpx 28rpx;
  margin-bottom: 24rpx;

  &__title {
    font-size: 30rpx;
    font-weight: 700;
    padding: 12rpx 0 8rpx;
  }
}

.form-row {
  display: flex;
  align-items: center;
  min-height: 100rpx;
  border-bottom: 1rpx solid #f2f3f5;

  &--last {
    border-bottom: none;
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

.chips {
  display: flex;
  flex-wrap: wrap;
  gap: 20rpx;
  padding: 16rpx 0 24rpx;

  &__item {
    padding: 14rpx 28rpx;
    border-radius: 32rpx;
    background: #f7f8fa;
    font-size: 26rpx;
    color: #4e5969;
    border: 2rpx solid transparent;

    &--active {
      background: $theme-color-light;
      color: $theme-color;
      border-color: $theme-color;
      font-weight: 600;
    }
  }

  &__empty {
    color: #c0c4cc;
    font-size: 24rpx;
    padding: 8rpx 0;
  }
}

.upload-box {
  margin: 16rpx 0 24rpx;
  height: 180rpx;
  border: 2rpx dashed #dcdfe6;
  border-radius: 16rpx;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  gap: 12rpx;
  color: #c0c4cc;
  font-size: 24rpx;
}
</style>
