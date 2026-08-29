package com.recycle;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@EnableScheduling
@SpringBootApplication(scanBasePackages = "com.recycle")
public class RecycleApplication {

    public static void main(String[] args) {
        // 业务全链路按北京时间（营业时间 openNow、工作台"今日"统计、单号/时间戳落库），
        // 与 jackson time-zone GMT+8、jdbc serverTimezone=Asia/Shanghai 保持一致，避免宿主机 UTC 偏移 8 小时
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Shanghai"));
        SpringApplication.run(RecycleApplication.class, args);
    }
}
