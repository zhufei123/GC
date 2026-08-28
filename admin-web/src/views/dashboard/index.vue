<script setup lang="ts">
import { onMounted, ref } from 'vue'

import { getDashboardSummary, type DashboardSummary } from '@/api/dashboard'

interface MetricCard {
  key: keyof DashboardSummary
  label: string
  icon: string
  color: string
  unit?: string
}

const cards: MetricCard[] = [
  { key: 'todayOrderCount', label: '今日订单数', icon: 'Tickets', color: '#07c160', unit: '单' },
  { key: 'todayWeightKg', label: '今日回收重量', icon: 'ScaleToOriginal', color: '#409eff', unit: 'kg' },
  { key: 'todayAmount', label: '今日交易金额', icon: 'Money', color: '#e6a23c', unit: '元' },
  { key: 'totalUserCount', label: '累计用户', icon: 'User', color: '#8e44ad', unit: '人' },
  { key: 'totalStoreCount', label: '回收站总数', icon: 'Shop', color: '#16a085', unit: '家' },
  { key: 'pendingApplyCount', label: '待审核入驻', icon: 'Bell', color: '#f56c6c', unit: '条' },
]

const loading = ref(false)
const summary = ref<Partial<DashboardSummary>>({})

function display(key: keyof DashboardSummary): string {
  const value = summary.value[key]
  return value === undefined || value === null || value === '' ? '-' : String(value)
}

async function loadData(): Promise<void> {
  loading.value = true
  try {
    summary.value = (await getDashboardSummary()) ?? {}
  } catch {
    // 拦截器已提示
  } finally {
    loading.value = false
  }
}

onMounted(loadData)
</script>

<template>
  <div class="page-container dashboard">
    <div class="welcome-card">
      <div class="welcome-text">
        <h2>工作台</h2>
        <p>欢迎回来,今天也是绿色环保的一天 🌱</p>
      </div>
      <el-button :loading="loading" circle @click="loadData">
        <el-icon v-if="!loading"><Refresh /></el-icon>
      </el-button>
    </div>

    <div v-loading="loading" class="metric-grid">
      <div v-for="card in cards" :key="card.key" class="metric-card">
        <div class="metric-icon" :style="{ backgroundColor: card.color + '1a', color: card.color }">
          <el-icon :size="26"><component :is="card.icon" /></el-icon>
        </div>
        <div class="metric-body">
          <div class="metric-label">{{ card.label }}</div>
          <div class="metric-value">
            {{ display(card.key) }}
            <span v-if="card.unit" class="metric-unit">{{ card.unit }}</span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<style scoped lang="scss">
.dashboard {
  .welcome-card {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 20px 24px;
    margin-bottom: 16px;
    border-radius: 12px;
    background: linear-gradient(120deg, #eafcf2, #ffffff 60%);
    border: 1px solid #d8f3e5;

    h2 {
      margin: 0 0 6px;
      font-size: 20px;
      color: #1f2d27;
    }

    p {
      margin: 0;
      font-size: 13px;
      color: #7d8f86;
    }
  }

  .metric-grid {
    display: grid;
    grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
    gap: 16px;
  }

  .metric-card {
    display: flex;
    align-items: center;
    gap: 16px;
    padding: 22px 20px;
    background: #fff;
    border-radius: 12px;
    border: 1px solid #eef2f0;
    box-shadow: 0 2px 8px rgba(31, 45, 39, 0.04);
    transition:
      transform 0.2s ease,
      box-shadow 0.2s ease;

    &:hover {
      transform: translateY(-3px);
      box-shadow: 0 8px 20px rgba(31, 45, 39, 0.08);
    }

    .metric-icon {
      display: flex;
      align-items: center;
      justify-content: center;
      width: 54px;
      height: 54px;
      border-radius: 14px;
      flex-shrink: 0;
    }

    .metric-label {
      font-size: 13px;
      color: #8a9a91;
      margin-bottom: 6px;
    }

    .metric-value {
      font-size: 24px;
      font-weight: 700;
      color: #1f2d27;

      .metric-unit {
        font-size: 12px;
        font-weight: 400;
        color: #9aa8a0;
        margin-left: 4px;
      }
    }
  }
}
</style>
