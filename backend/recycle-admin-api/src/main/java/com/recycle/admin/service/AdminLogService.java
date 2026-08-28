package com.recycle.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.recycle.common.core.BizException;
import com.recycle.common.core.ErrorCode;
import com.recycle.common.core.PageQuery;
import com.recycle.common.core.PageResult;
import com.recycle.common.entity.system.SysLog;
import com.recycle.common.mapper.SysLogMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class AdminLogService {

    private final SysLogMapper logMapper;

    public PageResult<SysLog> page(String module, String type, PageQuery query) {
        return PageResult.of(logMapper.selectPage(query.toPage(),
                new LambdaQueryWrapper<SysLog>()
                        .eq(StringUtils.hasText(module), SysLog::getModule, module)
                        .eq(StringUtils.hasText(type), SysLog::getType, type)
                        .orderByDesc(SysLog::getCreateTime)
                        .orderByDesc(SysLog::getId)));
    }

    public SysLog detail(Long id) {
        SysLog log = logMapper.selectById(id);
        if (log == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "日志不存在");
        }
        return log;
    }
}
