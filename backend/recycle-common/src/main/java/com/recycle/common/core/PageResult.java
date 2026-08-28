package com.recycle.common.core;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.util.List;
import java.util.function.Function;

/**
 * 分页响应：{total, pages, list}
 */
@Data
public class PageResult<T> {

    private long total;
    private long pages;
    private List<T> list;

    public static <T> PageResult<T> of(IPage<T> page) {
        PageResult<T> result = new PageResult<>();
        result.setTotal(page.getTotal());
        result.setPages(page.getPages());
        result.setList(page.getRecords());
        return result;
    }

    public static <S, T> PageResult<T> of(IPage<S> page, Function<S, T> mapper) {
        PageResult<T> result = new PageResult<>();
        result.setTotal(page.getTotal());
        result.setPages(page.getPages());
        result.setList(page.getRecords().stream().map(mapper).toList());
        return result;
    }

    public static <T> PageResult<T> of(long total, long pages, List<T> list) {
        PageResult<T> result = new PageResult<>();
        result.setTotal(total);
        result.setPages(pages);
        result.setList(list);
        return result;
    }
}
