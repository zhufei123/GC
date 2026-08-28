package com.recycle.common.core;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.Data;

/**
 * 分页查询基类：pageNum 默认 1，pageSize 默认 10，上限 100
 */
@Data
public class PageQuery {

    private Integer pageNum = 1;
    private Integer pageSize = 10;
    /** 管理端列表统一关键词（与具体字段别名并存） */
    private String keyword;
    /** yyyy-MM-dd，含当天 00:00:00 */
    private String beginDate;
    /** yyyy-MM-dd，含当天 23:59:59 */
    private String endDate;

    public <T> Page<T> toPage() {
        int num = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int size = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
        return new Page<>(num, size);
    }
}
