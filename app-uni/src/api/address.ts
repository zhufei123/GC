import { get, post, put, del } from "@/utils/request";

export interface AddressItem {
  id?: string;
  receiver: string;
  phone: string;
  province: string;
  city: string;
  district: string;
  street?: string;
  detail: string;
  longitude?: number;
  latitude?: number;
  isDefault?: boolean | number;
}

export function getAddressList() {
  return get<AddressItem[]>("/app-api/user/address/list", undefined, { silent: true });
}

export function createAddress(data: AddressItem) {
  return post("/app-api/user/address", data, { loading: true });
}

export function updateAddress(id: string, data: AddressItem) {
  return put(`/app-api/user/address/${id}`, data, { loading: true });
}

export function deleteAddress(id: string) {
  return del(`/app-api/user/address/${id}`);
}

export function setDefaultAddress(id: string) {
  return put(`/app-api/user/address/${id}/default`);
}
