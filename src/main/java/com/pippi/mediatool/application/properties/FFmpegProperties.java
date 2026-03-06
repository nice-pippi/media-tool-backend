package com.pippi.mediatool.application.properties;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * @Author: hong
 * @CreateTime: 2026-02-21
 * @Description: ffmpeg属性配置
 * @Version: 1.0
 */
@Data
@Component
@ConfigurationProperties(prefix = "ffmpeg")
public class FFmpegProperties {

    private String path;

}
