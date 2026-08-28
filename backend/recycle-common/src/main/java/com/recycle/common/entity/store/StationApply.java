package com.recycle.common.entity.store;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.recycle.common.entity.base.BaseEntity;
import com.recycle.common.util.JsonUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 入驻申请
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("station_apply")
public class StationApply extends BaseEntity {

    private Long userId;
    private String storeName;
    private String contactName;
    private String contactPhone;
    private String province;
    private String city;
    private String district;
    private String detail;
    private BigDecimal longitude;
    private BigDecimal latitude;
    private String licenseImage;
    /** JSON 数组字符串 */
    private String storeImages;
    /** JSON 数组字符串 */
    private String categoryIds;
    /** pending/approved/rejected */
    private String auditStatus;
    private String auditRemark;
    private Long auditorId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    private LocalDateTime auditTime;

    public String getPhone() {
        return contactPhone;
    }

    public void setPhone(String phone) {
        this.contactPhone = phone;
    }

    /** 兼容管理端 PENDING/pending */
    public String getStatus() {
        return auditStatus;
    }

    public List<String> getImages() {
        return JsonUtils.toStringList(storeImages);
    }

    public List<String> getLicenseImages() {
        if (licenseImage == null || licenseImage.isBlank()) {
            return List.of();
        }
        List<String> parsed = JsonUtils.toStringList(licenseImage);
        return parsed.isEmpty() ? List.of(licenseImage) : parsed;
    }

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "GMT+8")
    public LocalDateTime getAuditedAt() {
        return auditTime;
    }
}
