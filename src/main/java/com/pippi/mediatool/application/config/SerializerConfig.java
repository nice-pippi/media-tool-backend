package com.pippi.mediatool.application.config;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import com.fasterxml.jackson.datatype.jsr310.deser.LocalDateTimeDeserializer;
import com.fasterxml.jackson.datatype.jsr310.ser.LocalDateTimeSerializer;
import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/**
 * @Author: hong
 * @CreateTime: 2026-02-12
 * @Description: 序列化配置
 * @Version: 1.0
 */
@Configuration
public class SerializerConfig {

    private static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";

    @Bean
    public LocalDateTimeSerializer localDateTimeSerializer() {
        return new LocalDateTimeSerializer(DateTimeFormatter.ofPattern(DATE_TIME_PATTERN));
    }

    @Bean
    public LocalDateTimeDeserializer localDateTimeDeserializer() {
        return new LocalDateTimeDeserializer(DateTimeFormatter.ofPattern(DATE_TIME_PATTERN));
    }

    /**
     * Long转String避免精度损失
     */
    @Bean
    public ObjectMapper jacksonObjectMapper() {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);

        SimpleModule module = new SimpleModule();
        module.addSerializer(Long.class, ToStringSerializer.instance);
        module.addSerializer(Long.TYPE, ToStringSerializer.instance);
        // 添加LocalDateTime序列化支持
        module.addSerializer(LocalDateTime.class, localDateTimeSerializer());
        module.addDeserializer(LocalDateTime.class, localDateTimeDeserializer());

        objectMapper.registerModule(module);
        return objectMapper;
    }

    /**
     * 确保LocalDateTime对象在JSON序列化和反序列化时使用指定的格式。
     */
    @Bean
    public Jackson2ObjectMapperBuilderCustomizer jackson2ObjectMapperBuilderCustomizer() {
        return builder -> {
            builder.serializerByType(LocalDateTime.class, localDateTimeSerializer());
            builder.deserializerByType(LocalDateTime.class, localDateTimeDeserializer());
            // Long转String配置
            builder.serializerByType(Long.class, ToStringSerializer.instance);
            builder.serializerByType(Long.TYPE, ToStringSerializer.instance);
            // 配置不序列化null值
            builder.serializationInclusion(JsonInclude.Include.NON_NULL);
        };
    }
}