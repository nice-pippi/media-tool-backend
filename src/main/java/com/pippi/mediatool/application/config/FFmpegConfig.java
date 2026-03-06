package com.pippi.mediatool.application.config;

import com.pippi.mediatool.application.properties.FFmpegProperties;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFprobe;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

/**
 * @Author: hong
 * @CreateTime: 2026-02-21
 * @Description: ffmpeg配置类
 * @Version: 1.0
 */
@Configuration
public class FFmpegConfig {

    /**
     * 创建ffmpeg实例
     *
     * @param fFmpegProperties ffmpeg属性
     * @return FFmpeg实例
     * @throws IOException 创建实例异常
     */
    @Bean
    public FFmpeg ffmpeg(FFmpegProperties fFmpegProperties) throws IOException {
        return new FFmpeg(fFmpegProperties.getPath() + "/ffmpeg");
    }

    /**
     * 创建ffprobe实例
     *
     * @param fFmpegProperties ffmpeg属性
     * @return FFprobe实例
     * @throws IOException 创建实例异常
     */
    @Bean
    public FFprobe ffprobe(FFmpegProperties fFmpegProperties) throws IOException {
        return new FFprobe(fFmpegProperties.getPath() + "/ffprobe");
    }
}
