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

    public <T> Page<T> toPage() {
        int num = pageNum == null || pageNum < 1 ? 1 : pageNum;
        int size = pageSize == null || pageSize < 1 ? 10 : Math.min(pageSize, 100);
        return new Page<>(num, size);
    }
}
