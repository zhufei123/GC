import { get, post } from "@/utils/request";
import type { LoginPayload } from "@/store/user";

export function sendSmsCode(phone: string) {
  return post("/app-api/auth/sms-code", { phone });
}

export function phoneLogin(data: { phone: string; smsCode: string; client: "user" | "boss" }) {
  return post<LoginPayload>("/app-api/auth/phone-login", data);
}

/** 微信登录：小程序传 uni.login 的 code；H5 联调传 mock code（后端未配置 appid 时 code 即 openid） */
export function wxLogin(code: string) {
  return post<LoginPayload>("/app-api/auth/wx-login", { code });
}

/** 支付宝登录：mock 下 authCode 即 openid */
export function alipayLogin(authCode: string) {
  return post<LoginPayload>("/app-api/auth/alipay-login", { authCode });
}

/** 三方登录后补绑手机号；号码已注册则合并账号并返回新 token */
export function bindPhone(data: { phone: string; smsCode: string }) {
  return post<LoginPayload>("/app-api/user/bind-phone", data);
}

export function logout() {
  return post("/app-api/auth/logout", {}, { silent: true });
}

export function getMe() {
  return get("/app-api/user/me");
}
