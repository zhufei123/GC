package com.recycle.app.service;

import com.recycle.common.entity.member.NotifyLog;
import com.recycle.common.mapper.NotifyLogMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * 通知服务：站内信写 notify_log（channel=INAPP）。
 * 小程序订阅消息/支付宝消息由前端 requestSubscribeMessage 授权，正式接入后可在此扩展 WX/ALIPAY 渠道。
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotifyService {

    private final NotifyLogMapper notifyLogMapper;

    /** 站内信：始终写 notify_log */
    public void inApp(Long userId, String templateKey, String title, String content, String bizType, Long bizId) {
        NotifyLog notifyLog = new NotifyLog();
        notifyLog.setUserId(userId);
        notifyLog.setChannel("INAPP");
        notifyLog.setTemplateKey(templateKey);
        notifyLog.setBizType(bizType);
        notifyLog.setBizId(bizId);
        notifyLog.setTitle(title);
        notifyLog.setContent(content);
        notifyLog.setStatus("SENT");
        notifyLogMapper.insert(notifyLog);
    }

    /** 尽力而为：通知失败不阻断业务主流程 */
    public void inAppQuietly(Long userId, String templateKey, String title, String content, String bizType, Long bizId) {
        try {
            inApp(userId, templateKey, title, content, bizType, bizId);
        } catch (Exception e) {
            log.warn("[notify] inApp failed userId={} templateKey={} bizId={}", userId, templateKey, bizId, e);
        }
    }
}
