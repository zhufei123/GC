package com.recycle.common.entity.trade;

import com.baomidou.mybatisplus.annotation.TableName;
import com.recycle.common.entity.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * 打款单（C2B：回收站向客户付款）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("payout_order")
public class PayoutOrder extends BaseEntity {

    private String payoutNo;
    private Long orderId;
    private Long userId;
    private Long stationId;
    /** OFFLINE/WX_TRANSFER/ALIPAY_TRANSFER/WALLET */
    private String channel;
    private BigDecimal amount;
    /** 转账收款方 openid（WX/ALIPAY 渠道） */
    private String openid;
    /** 打款使用的小程序 appid，须与 openid 同应用（微信商家转账必填） */
    private String appid;
    /** SUCCESS/PROCESSING/WAIT_USER_CONFIRM/FAILED */
    private String status;
    private String channelBillNo;
    /** 微信商家转账用户确认收款 package 信息 */
    private String packageInfo;
    private String failReason;
}
