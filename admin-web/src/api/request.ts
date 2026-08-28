import axios, { type AxiosRequestConfig, type AxiosResponse } from 'axios'
import { ElMessage } from 'element-plus'

import type { R } from '@/types/api'
import { getToken, removeToken } from '@/utils/auth'

const service = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL,
  timeout: 15000,
})

/** 请求拦截:Authorization: {token},不加 Bearer */
service.interceptors.request.use((config) => {
  const token = getToken()
  if (token) {
    config.headers.Authorization = token
  }
  return config
})

/** 40100 跳登录防抖 */
let redirecting = false

function redirectToLogin(): void {
  if (redirecting) return
  redirecting = true
  removeToken()
  const current = location.pathname + location.search
  const query =
    current && current !== '/' && !current.startsWith('/login')
      ? `?redirect=${encodeURIComponent(current)}`
      : ''
  // 整页跳转,顺带重置 pinia / 动态路由
  location.href = `/login${query}`
}

/** 响应拦截:code===0 解包 data;40100 清态跳登录;其余提示错误 */
service.interceptors.response.use(
  (response: AxiosResponse): AxiosResponse => {
    const res = response.data as R
    // 非 R 结构(如文件流)直接透传
    if (res === null || typeof res !== 'object' || !('code' in res)) {
      return response
    }
    if (res.code === 0) {
      return res.data as unknown as AxiosResponse
    }
    if (res.code === 40100) {
      ElMessage.error(res.msg || '登录已过期,请重新登录')
      redirectToLogin()
    } else {
      ElMessage.error(res.msg || '请求失败')
    }
    return Promise.reject(new Error(res.msg || `业务错误 ${res.code}`)) as never
  },
  (error) => {
    const status = error.response?.status as number | undefined
    if (status === 401) {
      redirectToLogin()
    } else {
      const msg =
        (error.response?.data as R | undefined)?.msg ||
        (error.code === 'ECONNABORTED' ? '请求超时' : '网络异常,请稍后重试')
      ElMessage.error(msg)
    }
    return Promise.reject(error)
  },
)

/** 拦截器已解包 data,这里收敛为 Promise<T> */
export function request<T = unknown>(config: AxiosRequestConfig): Promise<T> {
  return service.request(config) as unknown as Promise<T>
}

export default request
