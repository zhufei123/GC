/// <reference types="vite/client" />

declare module '*.vue' {
  import { DefineComponent } from 'vue'
  // eslint-disable-next-line @typescript-eslint/no-explicit-any, @typescript-eslint/ban-types
  const component: DefineComponent<{}, {}, any>
  export default component
}

interface ImportMetaEnv {
  readonly VITE_API_BASE_URL: string
  /** 地图提供商: amap | qqmap | osm(默认 amap,key 缺失时降级) */
  readonly VITE_MAP_PROVIDER: string
  readonly VITE_AMAP_KEY: string
  readonly VITE_AMAP_SECURITY_CODE: string
  readonly VITE_MAP_QQ_KEY: string
  /** 微信订阅消息模板 id(接单进度)，为空则跳过订阅 */
  readonly VITE_WX_TMPL_ACCEPT: string
  /** 微信订阅消息模板 id(已称重)，为空则跳过订阅 */
  readonly VITE_WX_TMPL_WEIGHED: string
  /** 微信订阅消息模板 id(已完成)，为空则跳过订阅 */
  readonly VITE_WX_TMPL_COMPLETED: string
  /** 支付宝订阅消息 entityId(接单进度)，为空则跳过订阅 */
  readonly VITE_ALIPAY_TMPL_ACCEPT: string
  /** 支付宝订阅消息 entityId(已称重)，为空则跳过订阅 */
  readonly VITE_ALIPAY_TMPL_WEIGHED: string
  /** 支付宝订阅消息 entityId(已完成)，为空则跳过订阅 */
  readonly VITE_ALIPAY_TMPL_COMPLETED: string
  /** 微信商户号(requestMerchantTransfer 确认收款用)，为空则不传 mchId */
  readonly VITE_WX_MCH_ID: string
}

interface ImportMeta {
  readonly env: ImportMetaEnv
}
