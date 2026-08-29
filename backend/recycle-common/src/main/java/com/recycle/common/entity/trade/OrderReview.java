package com.recycle.common.entity.trade;

import com.baomidou.mybatisplus.annotation.TableName;
import com.recycle.common.entity.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("order_review")
public class OrderReview extends BaseEntity {

    private Long orderId;
    private Long userId;
    /** 冗余门店 id，便于统计门店均分 */
    private Long stationId;
    /** 1-5 星 */
    private Integer rating;
    private String comment;
    /** PENDING/APPROVED/REJECTED；有文字评论需审核 */
    private String auditStatus;
    private String auditRemark;
    private java.time.LocalDateTime auditedAt;
}
