package com.recycle;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "com.recycle")
public class RecycleApplication {

    public static void main(String[] args) {
        SpringApplication.run(RecycleApplication.class, args);
    }
}
