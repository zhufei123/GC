package com.recycle.app.support;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.recycle.app.vo.OrderItemVO;
import com.recycle.app.vo.OrderVO;
import com.recycle.common.entity.trade.OrderItem;
import com.recycle.common.entity.trade.PayoutOrder;
import com.recycle.common.entity.trade.RecycleOrder;
import com.recycle.common.mapper.OrderItemMapper;
import com.recycle.common.mapper.PayoutOrderMapper;
import com.recycle.common.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.List;

/**
 * 订单实体 → VO（含明细拆分 ESTIMATE/ACTUAL），C 端与 B 端共用
 */
@Component
@RequiredArgsConstructor
public class OrderAssembler {

    private final OrderItemMapper orderItemMapper;
    private final PayoutOrderMapper payoutOrderMapper;

    public OrderVO toVO(RecycleOrder order, boolean withItems, boolean maskPhone) {
        OrderVO vo = new OrderVO();
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
        vo.setPayMethod(order.getPayMethod());
        vo.setPayoutStatus(order.getPayoutStatus());
        vo.setPaidAt(order.getPaidAt());
        vo.setCreateTime(order.getCreateTime());
        vo.setAcceptedAt(order.getAcceptedAt());
        vo.setServedAt(order.getServedAt());
        vo.setWeighedAt(order.getWeighedAt());
        vo.setCompletedAt(order.getCompletedAt());
        vo.setCancelledAt(order.getCancelledAt());
        if (withItems) {
            List<OrderItem> items = orderItemMapper.selectList(new LambdaQueryWrapper<OrderItem>()
                    .eq(OrderItem::getOrderId, order.getId()));
            vo.setEstimateItems(items.stream()
                    .filter(i -> "ESTIMATE".equals(i.getItemType()))
                    .map(this::toItemVO).toList());
            vo.setActualItems(items.stream()
                    .filter(i -> "ACTUAL".equals(i.getItemType()))
                    .map(this::toItemVO).toList());
            // 详情补充打款单 package_info（微信商家转账确认收款用）
            if (StringUtils.hasText(order.getPayoutStatus())) {
                PayoutOrder payout = payoutOrderMapper.selectOne(new LambdaQueryWrapper<PayoutOrder>()
                        .eq(PayoutOrder::getOrderId, order.getId()));
                if (payout != null) {
                    vo.setPackageInfo(payout.getPackageInfo());
                }
            }
        }
        return vo;
    }

    private OrderItemVO toItemVO(OrderItem item) {
        OrderItemVO vo = new OrderItemVO();
        vo.setSkuId(item.getSkuId());
        vo.setSkuName(item.getSkuName());
        vo.setUnit(item.getUnit());
        vo.setWeight(item.getWeight());
        vo.setPrice(item.getPrice());
        vo.setAmount(item.getAmount());
        return vo;
    }

    public static String mask(String phone) {
        if (phone == null || phone.length() < 7) {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
