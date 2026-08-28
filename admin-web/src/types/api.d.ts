/** 后端统一响应 R<T> */
export interface R<T = unknown> {
  code: number
  msg: string
  data: T
  ts: number
}

/** 分页响应 */
export interface PageResult<T = unknown> {
  total: number
  pages: number
  list: T[]
}

/** 分页查询参数 */
export interface PageQuery {
  pageNum?: number
  pageSize?: number
  [key: string]: unknown
}
