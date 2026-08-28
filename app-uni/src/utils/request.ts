import { useUserStore } from "@/store/user";

/** 后端统一响应结构 */
export interface ApiResult<T = any> {
  code: number;
  msg: string;
  data: T;
  ts?: number;
}

export interface PageResult<T = any> {
  total: number;
  pages: number;
  list: T[];
}

let BASE_URL = "";
// #ifdef H5
// H5 开发态走 vite proxy，规避跨域
BASE_URL = import.meta.env.DEV ? "/api" : import.meta.env.VITE_API_BASE_URL || "";
// #endif
// #ifndef H5
BASE_URL = import.meta.env.VITE_API_BASE_URL || "";
// #endif

export interface RequestOptions {
  url: string;
  method?: "GET" | "POST" | "PUT" | "DELETE";
  data?: any;
  /** 是否显示 loading，默认 false */
  loading?: boolean;
  /** 是否吞掉业务错误(不 toast，由调用方处理)，默认 false */
  silent?: boolean;
}

let kickingToLogin = false;
function kickToLogin() {
  if (kickingToLogin) return;
  kickingToLogin = true;
  useUserStore().logout();
  uni.reLaunch({ url: "/pages/login/index" });
  setTimeout(() => {
    kickingToLogin = false;
  }, 2000);
}

export function request<T = any>(opts: RequestOptions): Promise<T> {
  const userStore = useUserStore();
  if (opts.loading) {
    uni.showLoading({ title: "加载中", mask: true });
  }
  return new Promise<T>((resolve, reject) => {
    uni.request({
      url: BASE_URL + opts.url,
      method: (opts.method || "GET") as any,
      data: opts.data,
      header: {
        "Content-Type": "application/json",
        ...(userStore.token ? { Authorization: userStore.token } : {}),
      },
      success: (res) => {
        const body = res.data as ApiResult<T>;
        if (!body || typeof body.code !== "number") {
          if (!opts.silent) uni.showToast({ title: "服务响应异常", icon: "none" });
          reject(new Error("bad response"));
          return;
        }
        if (body.code === 0) {
          resolve(body.data);
          return;
        }
        if (body.code === 40100) {
          kickToLogin();
          reject(body);
          return;
        }
        if (!opts.silent) {
          uni.showToast({ title: body.msg || "请求失败", icon: "none" });
        }
        reject(body);
      },
      fail: (err) => {
        if (!opts.silent) {
          uni.showToast({ title: "网络异常，请稍后重试", icon: "none" });
        }
        reject(err);
      },
      complete: () => {
        if (opts.loading) uni.hideLoading();
      },
    });
  });
}

export const get = <T = any>(url: string, data?: any, opts?: Partial<RequestOptions>) =>
  request<T>({ url, method: "GET", data, ...opts });
export const post = <T = any>(url: string, data?: any, opts?: Partial<RequestOptions>) =>
  request<T>({ url, method: "POST", data, ...opts });
export const put = <T = any>(url: string, data?: any, opts?: Partial<RequestOptions>) =>
  request<T>({ url, method: "PUT", data, ...opts });
export const del = <T = any>(url: string, data?: any, opts?: Partial<RequestOptions>) =>
  request<T>({ url, method: "DELETE", data, ...opts });
