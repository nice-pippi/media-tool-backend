package com.pippi.mediatool;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * @Author: hong
 * @CreateTime: 2026-02-10
 * @Description: 启动类
 * @Version: 1.0
 */
@EnableScheduling
@SpringBootApplication
public class MediaToolApplication {

    public static void main(String[] args) {
        SpringApplication.run(MediaToolApplication.class, args);
    }

}
