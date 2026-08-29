package com.recycle.app.pay;

import com.recycle.common.core.BizException;
import com.recycle.common.core.ErrorCode;
import com.recycle.common.util.JsonUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * 小程序订阅消息（需用户先 requestSubscribeMessage 授权，模板 id 配在 app.wx.subscribe-templates）。
 * 官方必填：touser、template_id、data、miniprogram_state、lang；page 建议填写。
 * https://developers.weixin.qq.com/miniprogram/dev/OpenApiDoc/mp-message-management/subscribe-message/sendMessage.html
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class WxSubscribeClient {

    private static final String SEND_URL =
            "https://api.weixin.qq.com/cgi-bin/message/subscribe/send?access_token={token}";

    private final WxPayProperties props;
    private final WxApiClient wxApiClient;
    private final RestClient restClient = RestClient.create();

    /** data: 模板字段名 -> 值（如 thing1 -> {"value": "xx"}） */
    public void send(String openid, String templateId, String page, Map<String, Object> data) {
        if (!props.loginConfigured()) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "微信订阅消息未配置 appid+secret");
        }
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("touser", openid);
        payload.put("template_id", templateId);
        payload.put("page", StringUtils.hasText(page) ? page : props.resolveSubscribePage(null));
        payload.put("data", data == null ? Map.of() : data);
        payload.put("miniprogram_state", StringUtils.hasText(props.getMiniprogramState())
                ? props.getMiniprogramState() : "formal");
        payload.put("lang", StringUtils.hasText(props.getLang()) ? props.getLang() : "zh_CN");
        String body;
        try {
            body = restClient.post()
                    .uri(SEND_URL, wxApiClient.stableToken())
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(payload)
                    .retrieve()
                    .body(String.class);
        } catch (BizException e) {
            throw e;
        } catch (Exception e) {
            throw new BizException(ErrorCode.SYSTEM_ERROR, "微信订阅消息发送失败：" + e.getMessage());
        }
        Map<String, Object> result = JsonUtils.toMap(body);
        Object errcode = result.get("errcode");
        if (errcode instanceof Number n && n.intValue() != 0) {
            throw new BizException(ErrorCode.SYSTEM_ERROR,
                    "微信订阅消息发送失败：" + result.get("errmsg"));
        }
    }
}
