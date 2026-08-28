package com.recycle.common.log;

import com.recycle.common.core.BizException;
import com.recycle.common.core.R;
import com.recycle.common.entity.system.SysLog;
import com.recycle.common.mapper.SysLogMapper;
import com.recycle.common.satoken.StpKit;
import com.recycle.common.util.JsonUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.regex.Pattern;

/**
 * @OpLog 切面：记录管理端操作日志（异步、脱敏 password/token/secret）
 */
@Slf4j
@Aspect
@Component
@RequiredArgsConstructor
public class OpLogAspect {

    private static final ExecutorService EXECUTOR = Executors.newVirtualThreadPerTaskExecutor();
    private static final Pattern SENSITIVE =
            Pattern.compile("(\"[^\"]*(?:password|token|secret)[^\"]*\"\\s*:\\s*\")[^\"]*(\")", Pattern.CASE_INSENSITIVE);

    private final SysLogMapper sysLogMapper;

    @Around("@annotation(opLog)")
    public Object around(ProceedingJoinPoint pjp, OpLog opLog) throws Throwable {
        long start = System.currentTimeMillis();
        SysLog entry = new SysLog();
        entry.setModule(opLog.module());
        entry.setType(opLog.type());
        entry.setDescription(StringUtils.hasText(opLog.value())
                ? opLog.value() : pjp.getSignature().getName());
        fillRequestInfo(entry, pjp);

        int resultCode = 0;
        try {
            Object result = pjp.proceed();
            if (result instanceof R<?> r) {
                resultCode = r.getCode();
            }
            return result;
        } catch (BizException e) {
            resultCode = e.getCode();
            throw e;
        } catch (Throwable t) {
            resultCode = 50000;
            throw t;
        } finally {
            entry.setResultCode(resultCode);
            entry.setCostMs((int) (System.currentTimeMillis() - start));
            entry.setCreateTime(LocalDateTime.now());
            EXECUTOR.execute(() -> {
                try {
                    sysLogMapper.insert(entry);
                } catch (Exception e) {
                    log.warn("save op log failed: {}", e.getMessage());
                }
            });
        }
    }

    private void fillRequestInfo(SysLog entry, ProceedingJoinPoint pjp) {
        try {
            ServletRequestAttributes attrs =
                    (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
            if (attrs != null) {
                HttpServletRequest request = attrs.getRequest();
                entry.setMethod(request.getMethod());
                entry.setPath(request.getRequestURI());
                entry.setIp(clientIp(request));
            }
            Object[] loggableArgs = Arrays.stream(pjp.getArgs())
                    .filter(a -> a != null
                            && !(a instanceof HttpServletRequest)
                            && !(a instanceof HttpServletResponse)
                            && !(a instanceof MultipartFile))
                    .toArray();
            String params = JsonUtils.toJson(loggableArgs);
            if (params != null) {
                if (params.length() > 2000) {
                    params = params.substring(0, 2000);
                }
                entry.setParams(SENSITIVE.matcher(params).replaceAll("$1***$2"));
            }
            if (StpKit.ADMIN.isLogin()) {
                entry.setOperatorId(StpKit.ADMIN.getLoginIdAsLong());
                entry.setOperator("admin:" + StpKit.ADMIN.getLoginIdAsString());
            }
        } catch (Exception e) {
            log.debug("fill op log request info failed: {}", e.getMessage());
        }
    }

    private String clientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (StringUtils.hasText(ip)) {
            return ip.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        return StringUtils.hasText(realIp) ? realIp : request.getRemoteAddr();
    }
}
