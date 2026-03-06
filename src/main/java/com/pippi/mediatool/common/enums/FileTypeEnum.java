package com.pippi.mediatool.common.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * @Author: hong
 * @CreateTime: 2026-02-24
 * @Description: 文件类型枚举
 * @Version: 1.0
 */
@Getter
@AllArgsConstructor
public enum FileTypeEnum {
    IMAGE(0, "图片"),
    VIDEO(1, "视频"),
    AUDIO(2, "音频"),
    ;

    private final Integer code;

    private final String description;

}
