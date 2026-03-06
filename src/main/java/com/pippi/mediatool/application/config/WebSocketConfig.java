package com.pippi.mediatool.application.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.server.standard.ServerEndpointExporter;

/**
 * @Author: hong
 * @CreateTime: 2026-02-23
 * @Description: WebSocket配置类
 * @Version: 1.0
 */
@Configuration
public class WebSocketConfig {

    /**
     * 自动注册使用@ServerEndpoint注解的类
     */
    @Bean
    public ServerEndpointExporter serverEndpointExporter() {
        return new ServerEndpointExporter();
    }

}
