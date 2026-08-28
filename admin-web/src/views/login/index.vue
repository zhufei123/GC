<script setup lang="ts">
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'

import { useUserStore } from '@/store/modules/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const formRef = ref<FormInstance>()
const loading = ref(false)

const form = reactive({
  username: 'admin',
  password: 'Admin@123',
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }],
}

async function handleLogin(): Promise<void> {
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) return

  loading.value = true
  try {
    await userStore.loginAction({ ...form })
    ElMessage.success('登录成功')
    const redirect = (route.query.redirect as string) || '/'
    router.replace(redirect)
  } catch {
    // 错误提示由 request 拦截器统一处理
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <div class="login-page">
    <!-- 左侧品牌区 -->
    <div class="brand-panel">
      <div class="brand-inner">
        <div class="brand-logo">
          <el-icon :size="46"><Refresh /></el-icon>
        </div>
        <h1 class="brand-title">绿色回收管理后台</h1>
        <p class="brand-slogan">让每一件可回收物,都物尽其用</p>
        <ul class="brand-points">
          <li><el-icon><CircleCheckFilled /></el-icon>上门回收 · 到店回收全流程管理</li>
          <li><el-icon><CircleCheckFilled /></el-icon>品类价格实时调控,调价留痕</li>
          <li><el-icon><CircleCheckFilled /></el-icon>回收站入驻审核,订单全链路可视</li>
        </ul>
      </div>
      <div class="circle circle-1" />
      <div class="circle circle-2" />
      <div class="circle circle-3" />
    </div>

    <!-- 右侧表单区 -->
    <div class="form-panel">
      <div class="form-card">
        <div class="form-head">
          <div class="form-logo">
            <el-icon :size="30"><Refresh /></el-icon>
          </div>
          <h2>欢迎登录</h2>
          <p>绿色回收 · 运营管理平台</p>
        </div>

        <el-form
          ref="formRef"
          :model="form"
          :rules="rules"
          size="large"
          @keyup.enter="handleLogin"
        >
          <el-form-item prop="username">
            <el-input
              v-model.trim="form.username"
              placeholder="用户名"
              :prefix-icon="'User'"
              clearable
            />
          </el-form-item>
          <el-form-item prop="password">
            <el-input
              v-model.trim="form.password"
              type="password"
              placeholder="密码"
              :prefix-icon="'Lock'"
              show-password
            />
          </el-form-item>
          <el-form-item>
            <el-button
              class="login-btn"
              type="primary"
              size="large"
              :loading="loading"
              @click="handleLogin"
            >
              登 录
            </el-button>
          </el-form-item>
        </el-form>

        <div class="form-tip">测试账号:admin / Admin@123</div>
      </div>
      <div class="copyright">© 2026 绿色回收 GreenRecycle</div>
    </div>
  </div>
</template>

<style scoped lang="scss">
@use '@/styles/variables.scss' as *;

.login-page {
  display: flex;
  width: 100%;
  height: 100%;
  overflow: hidden;
}

/* ---------- 左侧品牌区 ---------- */
.brand-panel {
  position: relative;
  flex: 1.2;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #059a4e 0%, #07c160 55%, #3ad68b 100%);
  color: #fff;
  overflow: hidden;

  .brand-inner {
    position: relative;
    z-index: 2;
    max-width: 460px;
    padding: 0 40px;
  }

  .brand-logo {
    display: inline-flex;
    align-items: center;
    justify-content: center;
    width: 84px;
    height: 84px;
    border-radius: 24px;
    background: rgba(255, 255, 255, 0.18);
    backdrop-filter: blur(6px);
    margin-bottom: 28px;
  }

  .brand-title {
    margin: 0 0 12px;
    font-size: 34px;
    font-weight: 700;
    letter-spacing: 2px;
  }

  .brand-slogan {
    margin: 0 0 36px;
    font-size: 16px;
    opacity: 0.92;
    letter-spacing: 1px;
  }

  .brand-points {
    list-style: none;
    margin: 0;
    padding: 0;
    display: flex;
    flex-direction: column;
    gap: 16px;

    li {
      display: flex;
      align-items: center;
      gap: 10px;
      font-size: 15px;
      opacity: 0.95;
    }
  }

  .circle {
    position: absolute;
    border-radius: 50%;
    background: rgba(255, 255, 255, 0.08);
  }

  .circle-1 {
    width: 420px;
    height: 420px;
    top: -140px;
    right: -120px;
  }

  .circle-2 {
    width: 260px;
    height: 260px;
    bottom: -80px;
    left: -60px;
  }

  .circle-3 {
    width: 140px;
    height: 140px;
    bottom: 120px;
    right: 80px;
    background: rgba(255, 255, 255, 0.12);
  }
}

/* ---------- 右侧表单区 ---------- */
.form-panel {
  position: relative;
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f6faf8;

  .form-card {
    width: 380px;
    padding: 44px 40px 32px;
    background: #fff;
    border-radius: 16px;
    box-shadow: 0 12px 40px rgba(7, 193, 96, 0.1);
  }

  .form-head {
    text-align: center;
    margin-bottom: 30px;

    .form-logo {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      width: 56px;
      height: 56px;
      border-radius: 16px;
      color: #fff;
      background: linear-gradient(135deg, #07c160, #3ad68b);
      margin-bottom: 14px;
      box-shadow: 0 6px 16px rgba(7, 193, 96, 0.35);
    }

    h2 {
      margin: 0 0 6px;
      font-size: 22px;
      color: #1f2d27;
    }

    p {
      margin: 0;
      font-size: 13px;
      color: #8a9a91;
    }
  }

  .login-btn {
    width: 100%;
    letter-spacing: 6px;
    font-weight: 600;
  }

  .form-tip {
    margin-top: 6px;
    text-align: center;
    font-size: 12px;
    color: #9aa8a0;
  }

  .copyright {
    position: absolute;
    bottom: 20px;
    left: 0;
    right: 0;
    text-align: center;
    font-size: 12px;
    color: #b0bcb5;
  }
}

@media (max-width: 900px) {
  .brand-panel {
    display: none;
  }
}
</style>
