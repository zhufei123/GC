import { get, post } from "@/utils/request";
import type { LoginPayload } from "@/store/user";

export function sendSmsCode(phone: string) {
  return post("/app-api/auth/sms-code", { phone });
}

export function phoneLogin(data: { phone: string; smsCode: string; client: "user" | "boss" }) {
  return post<LoginPayload>("/app-api/auth/phone-login", data);
}

export function logout() {
  return post("/app-api/auth/logout", {}, { silent: true });
}

export function getMe() {
  return get("/app-api/user/me");
}
