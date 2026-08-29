package com.recycle.app.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Data
public class OrderVO {

    private Long id;
    private String orderNo;
    private Long userId;
    private Long stationId;
    private String stationName;
    private String type;
    private String status;
    private String receiver;
    private String phone;
    private String address;
    private BigDecimal longitude;
    private BigDecimal latitude;

    @JsonFormat(pattern = "yyyy-MM-dd", timezone = "GMT+8")
    private LocalDate appointDate;

    private String appointPeriod;
    private BigDecimal estimateAmount;
    private BigDecimal actualAmount;

    /** OFFLINE/WX_TRANSFER/ALIPAY_TRANSFER/WALLET */
    private String payMethod;

    /** SUCCESS/PROCESSING/WAIT_USER_CONFIRM/FAILED */
    private String payoutStatus;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime paidAt;

    /** 打款单号（详情返回，C 端凭此对账/模拟回调） */
    private String payoutNo;

    /** 打款失败原因（payoutStatus=FAILED 时详情返回） */
    private String payoutFailReason;

    /** 微信商家转账用户确认收款 package 信息（详情返回） */
    private String packageInfo;

    private List<String> images;
    private List<String> weighImages;
    private String remark;
    private String cancelBy;
    private String cancelReason;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime createTime;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime acceptedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime servedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime weighedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime completedAt;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime cancelledAt;

    /** 预估明细 */
    private List<OrderItemVO> estimateItems;

    /** 实收明细 */
    private List<OrderItemVO> actualItems;

    /** 兼容小程序称重页 items */
    public List<OrderItemVO> getItems() {
        if (actualItems != null && !actualItems.isEmpty()) {
            return actualItems;
        }
        return estimateItems;
    }
}
