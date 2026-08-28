<template>
  <view class="notice">
    <view v-if="list.length">
      <view v-for="item in list" :key="item.id" class="notice-card" @tap="goBiz(item)">
        <view class="notice-card__head">
          <view class="notice-card__icon">
            <wd-icon :name="iconOf(item)" size="34rpx" color="#07c160" />
          </view>
          <text class="notice-card__title">{{ item.title || "系统通知" }}</text>
          <text class="notice-card__time">{{ shortTime(item.createTime) }}</text>
        </view>
        <view v-if="item.content" class="notice-card__content">{{ item.content }}</view>
      </view>
      <view class="notice__more">
        <text v-if="finished">没有更多了</text>
        <text v-else-if="loading">加载中...</text>
        <text v-else @tap="loadMore">加载更多</text>
      </view>
    </view>
    <wd-status-tip v-else-if="!loading" image="content" tip="暂无消息" />
    <view v-if="loading && !list.length" class="notice__loading"><wd-loading color="#07c160" /></view>
  </view>
</template>

<script setup lang="ts">
import { ref } from "vue";
import { onShow, onReachBottom } from "@dcloudio/uni-app";
import { getUserNotices, type NoticeItem } from "@/api/user";

const PAGE_SIZE = 15;

const list = ref<NoticeItem[]>([]);
const pageNum = ref(1);
const loading = ref(false);
const finished = ref(false);

function iconOf(item: NoticeItem) {
  const key = item.templateKey || "";
  if (key.includes("COMPLETED")) return "check-circle";
  if (key.includes("CANCELLED")) return "close-circle";
  if (key.includes("WEIGHED")) return "chart-bar";
  return "notification";
}

function shortTime(time?: string) {
  if (!time) return "";
  return time.length > 16 ? time.slice(5, 16) : time;
}

function goBiz(item: NoticeItem) {
  if (item.bizType === "ORDER" && item.bizId) {
    uni.navigateTo({ url: `/pages-customer/order/detail?id=${item.bizId}` });
  }
}

async function load(reset = false) {
  if (loading.value) return;
  loading.value = true;
  try {
    const page = reset ? 1 : pageNum.value;
    const res = await getUserNotices({ pageNum: page, pageSize: PAGE_SIZE });
    const rows = res?.list || [];
    list.value = reset ? rows : [...list.value, ...rows];
    pageNum.value = page + 1;
    finished.value = rows.length < PAGE_SIZE;
  } catch (e) {
    /* 静默失败，保留已有列表 */
  } finally {
    loading.value = false;
  }
}

function loadMore() {
  if (!finished.value) load();
}

onShow(() => load(true));
onReachBottom(loadMore);
</script>

<style lang="scss" scoped>
.notice {
  min-height: 100vh;
  background: $page-bg;
  padding: 24rpx 32rpx 48rpx;

  &__loading {
    display: flex;
    justify-content: center;
    padding: 120rpx 0;
  }

  &__more {
    text-align: center;
    padding: 24rpx 0;
    font-size: 24rpx;
    color: #86909c;
  }
}

.notice-card {
  background: #fff;
  border-radius: 24rpx;
  padding: 26rpx 28rpx;
  margin-bottom: 20rpx;

  &__head {
    display: flex;
    align-items: center;
    gap: 16rpx;
  }

  &__icon {
    width: 56rpx;
    height: 56rpx;
    border-radius: 16rpx;
    background: #e8f9ef;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
  }

  &__title {
    flex: 1;
    font-size: 29rpx;
    font-weight: 600;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  &__time {
    font-size: 22rpx;
    color: #c0c4cc;
    flex-shrink: 0;
  }

  &__content {
    margin-top: 16rpx;
    font-size: 26rpx;
    color: #4e5969;
    line-height: 1.6;
  }
}
</style>
