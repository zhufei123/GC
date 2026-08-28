<script setup lang="ts">
import { useRouter } from 'vue-router'

const router = useRouter()
</script>

<template>
  <div class="not-found">
    <div class="code">4<el-icon class="leaf" :size="88"><Refresh /></el-icon>4</div>
    <h2>页面不见了</h2>
    <p>您访问的页面不存在,或没有访问权限</p>
    <div class="actions">
      <el-button type="primary" @click="router.push('/')">回到首页</el-button>
      <el-button @click="router.back()">返回上一页</el-button>
    </div>
  </div>
</template>

<style scoped lang="scss">
.not-found {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  min-height: 60vh;
  color: #1f2d27;

  .code {
    display: flex;
    align-items: center;
    gap: 8px;
    font-size: 120px;
    font-weight: 800;
    color: #07c160;
    line-height: 1;

    .leaf {
      color: #3ad68b;
    }
  }

  h2 {
    margin: 24px 0 8px;
    font-size: 22px;
  }

  p {
    margin: 0 0 28px;
    color: #8a9a91;
  }
}
</style>
