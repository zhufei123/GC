package com.recycle.app.service;

import com.recycle.app.pay.AlipayMessageClient;
import com.recycle.app.pay.AlipayPayProperties;
import com.recycle.app.pay.WxPayProperties;
import com.recycle.app.pay.WxSubscribeClient;
import com.recycle.common.entity.member.NotifyLog;
import com.recycle.common.entity.member.User;
import com.recycle.common.mapper.NotifyLogMapper;
import com.recycle.common.mapper.UserMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 通知服务：站内信必写 notify_log（channel=INAPP）；
 * 用户绑定微信/支付宝且配置了对应模板 id 时，同步尝试渠道推送并记录 SENT/FAILED。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotifyService {

    private final NotifyLogMapper notifyLogMapper;
    private final UserMapper userMapper;
    private final WxPayProperties wxProps;
    private final AlipayPayProperties alipayProps;
    private final WxSubscribeClient wxSubscribeClient;
    private final AlipayMessageClient alipayMessageClient;

    /** 站内信：始终写 notify_log */
    public void inApp(Long userId, String templateKey, String title, String content, String bizType, Long bizId) {
        insertLog(userId, "INAPP", templateKey, title, content, bizType, bizId, "SENT", null);
    }

    /** 尽力而为：通知失败不阻断业务主流程 */
    public void inAppQuietly(Long userId, String templateKey, String title, String content, String bizType, Long bizId) {
        try {
            inApp(userId, templateKey, title, content, bizType, bizId);
        } catch (Exception e) {
            log.warn("[notify] inApp failed userId={} templateKey={} bizId={}", userId, templateKey, bizId, e);
        }
    }

    /** 站内信 + 已绑定渠道推送（微信订阅消息/支付宝消息，模板 id 未配置则跳过） */
    public void notifyUser(Long userId, String templateKey, String title, String content, String bizType, Long bizId) {
        inApp(userId, templateKey, title, content, bizType, bizId);
        User user = userMapper.selectById(userId);
        if (user == null) {
            return;
        }
        String wxTemplateId = wxProps.getSubscribeTemplates().get(templateKey);
        if (StringUtils.hasText(user.getOpenidWx()) && StringUtils.hasText(wxTemplateId)) {
            try {
                wxSubscribeClient.send(user.getOpenidWx(), wxTemplateId, null, templateData(title, content));
                insertLog(userId, "WX", templateKey, title, content, bizType, bizId, "SENT", null);
            } catch (Exception e) {
                log.warn("[notify] wx subscribe failed userId={} templateKey={}", userId, templateKey, e);
                insertLog(userId, "WX", templateKey, title, content, bizType, bizId, "FAILED", trim(e.getMessage()));
            }
        }
        String alipayTemplateId = alipayProps.getMessageTemplates().get(templateKey);
        if (StringUtils.hasText(user.getOpenidAlipay()) && StringUtils.hasText(alipayTemplateId)) {
            try {
                alipayMessageClient.send(user.getOpenidAlipay(), alipayTemplateId, null, templateData(title, content));
                insertLog(userId, "ALIPAY", templateKey, title, content, bizType, bizId, "SENT", null);
            } catch (Exception e) {
                log.warn("[notify] alipay message failed userId={} templateKey={}", userId, templateKey, e);
                insertLog(userId, "ALIPAY", templateKey, title, content, bizType, bizId, "FAILED", trim(e.getMessage()));
            }
        }
    }

    /** 尽力而为：通知失败不阻断业务主流程 */
    public void notifyUserQuietly(Long userId, String templateKey, String title, String content, String bizType, Long bizId) {
        try {
            notifyUser(userId, templateKey, title, content, bizType, bizId);
        } catch (Exception e) {
            log.warn("[notify] notifyUser failed userId={} templateKey={} bizId={}", userId, templateKey, bizId, e);
        }
    }

    private void insertLog(Long userId, String channel, String templateKey, String title, String content,
                           String bizType, Long bizId, String status, String error) {
        NotifyLog notifyLog = new NotifyLog();
        notifyLog.setUserId(userId);
        notifyLog.setChannel(channel);
        notifyLog.setTemplateKey(templateKey);
        notifyLog.setBizType(bizType);
        notifyLog.setBizId(bizId);
        notifyLog.setTitle(title);
        notifyLog.setContent(content);
        notifyLog.setStatus(status);
        notifyLog.setError(error);
        notifyLogMapper.insert(notifyLog);
    }

    /** 通用模板字段：thing 类字段限 20 字符 */
    private Map<String, Object> templateData(String title, String content) {
        Map<String, Object> data = new LinkedHashMap<>();
        data.put("thing1", Map.of("value", truncate(title, 20)));
        data.put("thing2", Map.of("value", truncate(content, 20)));
        return data;
    }

    private String truncate(String text, int max) {
        if (text == null) {
            return "";
        }
        return text.length() <= max ? text : text.substring(0, max - 1) + "…";
    }

    private String trim(String message) {
        return truncate(message, 190);
    }
}
