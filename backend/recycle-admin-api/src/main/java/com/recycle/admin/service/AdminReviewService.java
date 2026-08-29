package com.recycle.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.recycle.admin.dto.ReviewAuditDTO;
import com.recycle.common.core.BizException;
import com.recycle.common.core.ErrorCode;
import com.recycle.common.core.PageQuery;
import com.recycle.common.core.PageResult;
import com.recycle.common.entity.trade.OrderReview;
import com.recycle.common.mapper.OrderReviewMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class AdminReviewService {

    private static final Set<String> RESULTS = Set.of("APPROVED", "REJECTED");

    private final OrderReviewMapper orderReviewMapper;

    public PageResult<OrderReview> page(String auditStatus, Long stationId, PageQuery query) {
        return PageResult.of(orderReviewMapper.selectPage(query.toPage(),
                new LambdaQueryWrapper<OrderReview>()
                        .eq(StringUtils.hasText(auditStatus), OrderReview::getAuditStatus, auditStatus)
                        .eq(stationId != null, OrderReview::getStationId, stationId)
                        .orderByDesc(OrderReview::getId)));
    }

    public void audit(Long id, ReviewAuditDTO dto) {
        String status = dto.getStatus() == null ? "" : dto.getStatus().trim().toUpperCase();
        if (!RESULTS.contains(status)) {
            throw new BizException(ErrorCode.PARAM_ERROR, "审核结果只能是通过或拒绝");
        }
        OrderReview review = orderReviewMapper.selectById(id);
        if (review == null) {
            throw new BizException(ErrorCode.NOT_FOUND, "评价不存在");
        }
        int rows = orderReviewMapper.update(null, new LambdaUpdateWrapper<OrderReview>()
                .eq(OrderReview::getId, id)
                .set(OrderReview::getAuditStatus, status)
                .set(OrderReview::getAuditRemark, dto.getRemark())
                .set(OrderReview::getAuditedAt, LocalDateTime.now()));
        if (rows == 0) {
            throw new BizException(ErrorCode.NOT_FOUND, "评价不存在");
        }
    }
}
