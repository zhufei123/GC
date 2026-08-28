package com.recycle.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.recycle.admin.vo.AdminOrderVO;
import com.recycle.common.core.BizException;
import com.recycle.common.core.ErrorCode;
import com.recycle.common.core.PageQuery;
import com.recycle.common.core.PageResult;
import com.recycle.common.entity.store.RecycleStation;
import com.recycle.common.entity.trade.OrderItem;
import com.recycle.common.entity.trade.RecycleOrder;
import com.recycle.common.mapper.OrderItemMapper;
import com.recycle.common.mapper.RecycleOrderMapper;
import com.recycle.common.mapper.RecycleStationMapper;
import com.recycle.common.util.JsonUtils;
import com.recycle.common.util.QueryParams;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class AdminTradeService {

    private final RecycleOrderMapper orderMapper;
    private final OrderItemMapper orderItemMapper;
    private final RecycleStationMapper stationMapper;

    public PageResult<AdminOrderVO> page(String status, String orderNo, Long userId, PageQuery query) {
        LocalDateTime begin = QueryParams.startOfDay(query.getBeginDate());
        LocalDateTime endExclusive = QueryParams.startOfNextDay(query.getEndDate());
        String no = QueryParams.firstText(orderNo, query.getKeyword());
        String statusFilter = QueryParams.orderStatus(status);
        Page<RecycleOrder> page = orderMapper.selectPage(query.toPage(),
                new LambdaQueryWrapper<RecycleOrder>()
                        .eq(StringUtils.hasText(statusFilter), RecycleOrder::getStatus, statusFilter)
                        .like(StringUtils.hasText(no), RecycleOrder::getOrderNo, no)
                        .eq(userId != null, RecycleOrder::getUserId, userId)
                        .ge(begin != null, RecycleOrder::getCreateTime, begin)
                        .lt(endExclusive != null, RecycleOrder::getCreateTime, endExclusive)
                        .orderByDesc(RecycleOrder::getCreateTime));
        return PageResult.of(page, o -> toVO(o, false, true));
    }

    public AdminOrderVO detail(Long id) {
        RecycleOrder order = require(id);
        return toVO(order, true, false);
    }

    /** 后台取消：非终态皆可 */
    public void cancel(Long id, String reason) {
        RecycleOrder order = require(id);
        if ("COMPLETED".equals(order.getStatus()) || "CANCELLED".equals(order.getStatus())) {
            throw new BizException(ErrorCode.ORDER_STATUS_ILLEGAL);
        }
        int rows = orderMapper.update(null, new LambdaUpdateWrapper<RecycleOrder>()
                .eq(RecycleOrder::getId, id)
                .in(RecycleOrder::getStatus, List.of("PENDING", "ACCEPTED", "SERVING", "WEIGHED"))
                .set(RecycleOrder::getStatus, "CANCELLED")
                .set(RecycleOrder::getCancelBy, "admin")
                .set(RecycleOrder::getCancelReason, reason)
                .set(RecycleOrder::getCancelledAt, LocalDateTime.now()));
        if (rows == 0) {
            throw new BizException(ErrorCode.ORDER_STATUS_ILLEGAL);
        }
    }

    private RecycleOrder require(Long id) {
        RecycleOrder order = orderMapper.selectById(id);
        if (order == null) {
            throw new BizException(ErrorCode.ORDER_NOT_FOUND);
        }
        return order;
    }

    private AdminOrderVO toVO(RecycleOrder order, boolean withItems, boolean maskPhone) {
        AdminOrderVO vo = new AdminOrderVO();
        vo.setId(order.getId());
        vo.setOrderNo(order.getOrderNo());
        vo.setUserId(order.getUserId());
        vo.setStationId(order.getStationId());
        vo.setType(order.getType());
        vo.setStatus(order.getStatus());
        vo.setReceiver(order.getReceiver());
        vo.setPhone(maskPhone ? mask(order.getPhone()) : order.getPhone());
        vo.setAddress(order.getAddress());
        vo.setLongitude(order.getLongitude());
        vo.setLatitude(order.getLatitude());
        vo.setAppointDate(order.getAppointDate());
        vo.setAppointPeriod(order.getAppointPeriod());
        vo.setEstimateAmount(order.getEstimateAmount());
        vo.setActualAmount(order.getActualAmount());
        vo.setImages(JsonUtils.toStringList(order.getPhotosCustomer()));
        vo.setWeighImages(JsonUtils.toStringList(order.getPhotosWeigh()));
        vo.setRemark(order.getRemark());
        vo.setCancelBy(order.getCancelBy());
        vo.setCancelReason(order.getCancelReason());
        vo.setCreateTime(order.getCreateTime());
        vo.setAcceptedAt(order.getAcceptedAt());
        vo.setServedAt(order.getServedAt());
        vo.setWeighedAt(order.getWeighedAt());
        vo.setCompletedAt(order.getCompletedAt());
        vo.setCancelledAt(order.getCancelledAt());
        if (order.getStationId() != null) {
            RecycleStation station = stationMapper.selectById(order.getStationId());
            if (station != null) {
                vo.setStationName(station.getName());
            }
        }
        if (withItems) {
            List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                    .eq(OrderItem::getOrderId, order.getId()));
            vo.setEstimateItems(items.stream()
                    .filter(i -> "ESTIMATE".equals(i.getItemType())).map(this::toItemVO).toList());
            vo.setActualItems(items.stream()
                    .filter(i -> "ACTUAL".equals(i.getItemType())).map(this::toItemVO).toList());
            vo.setTimeline(buildTimeline(order));
        }
        return vo;
    }

    private AdminOrderVO.ItemVO toItemVO(OrderItem item) {
        AdminOrderVO.ItemVO vo = new AdminOrderVO.ItemVO();
        vo.setSkuId(item.getSkuId());
        vo.setSkuName(item.getSkuName());
        vo.setUnit(item.getUnit());
        vo.setWeight(item.getWeight());
        vo.setPrice(item.getPrice());
        vo.setAmount(item.getAmount());
        return vo;
    }

    private List<AdminOrderVO.TimelineVO> buildTimeline(RecycleOrder order) {
        List<AdminOrderVO.TimelineVO> timeline = new ArrayList<>();
        addNode(timeline, "PENDING", "下单", order.getCreateTime());
        addNode(timeline, "ACCEPTED", "接单", order.getAcceptedAt());
        addNode(timeline, "SERVING", "开始服务", order.getServedAt());
        addNode(timeline, "WEIGHED", "称重", order.getWeighedAt());
        addNode(timeline, "COMPLETED", "完成", order.getCompletedAt());
        if (order.getCancelledAt() != null || "CANCELLED".equals(order.getStatus())) {
            addNode(timeline, "CANCELLED", "取消", order.getCancelledAt());
        }
        return timeline;
    }

    /** 未到达的节点也下发，前端按当前状态点亮 */
    private void addNode(List<AdminOrderVO.TimelineVO> timeline, String status, String label, LocalDateTime time) {
        AdminOrderVO.TimelineVO node = new AdminOrderVO.TimelineVO();
        node.setStatus(status);
        node.setLabel(label);
        node.setTime(time);
        timeline.add(node);
    }

    private String mask(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
