package com.recycle.common.entity.member;

import com.baomidou.mybatisplus.annotation.TableName;
import com.recycle.common.entity.base.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

/**
 * C/B 端用户（role: customer/recycler）
 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("`user`")
public class User extends BaseEntity {

    /** 微信 openid，绑定 wxAppid（code2session / 商家转账 / 订阅消息） */
    private String openidWx;
    /** 颁发该 openid 的小程序 appid */
    private String wxAppid;
    private String unionidWx;
    /** code2session 会话密钥，仅服务端，禁止下发前端 */
    private String wxSessionKey;
    private java.time.LocalDateTime wxSessionAt;
    /** 支付宝 open_id（新商户推荐，模板消息 to_open_id） */
    private String openidAlipay;
    /** 颁发该标识的支付宝小程序 app_id */
    private String alipayAppId;
    /** 支付宝 user_id，2088 开头 16 位（存量接口 to_user_id / ALIPAY_USER_ID） */
    private String alipayUserId;
    /** oauth access_token，仅服务端（alipay.user.info.share） */
    private String alipayAccessToken;
    private String alipayRefreshToken;
    private java.time.LocalDateTime alipayTokenExpireAt;
    private String phone;
    private String nickname;
    private String avatar;
    /** 0未知 1男 2女（授权时） */
    private Integer gender;
    /** 资料城市（授权时） */
    private String city;
    private String province;
    private String country;
    private String language;
    /** 1=曾授权微信订阅消息 */
    private Integer subscribeWx;
    /** 1=曾授权支付宝订阅消息 */
    private Integer subscribeAlipay;
    /** customer/recycler */
    private String role;
    /** 1启用 0禁用 */
    private Integer status;
    /** 钱包预留 */
    private BigDecimal balance;
    /** none/pending/approved/rejected */
    private String recyclerStatus;
}
