package com.pippi.mediatool.common.utils;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.File;

/**
 * @Author: hong
 * @CreateTime: 2026-02-24
 * @Description: 文件工具类
 * @Version: 1.0
 */
@Slf4j
public class FileUtil {
    public static void makeDir(String tempPath) {
        File file = new File(tempPath);
        if (!file.exists()) {
            if (file.mkdirs()) {
                log.info("创建临时目录成功：{}", tempPath);
            }
        }
    }

    public static void deleteFile(String filePath) {
        if (StringUtils.isEmpty(filePath)) {
            return;
        }

        File file = new File(filePath);
        if (file.exists()) {
            if (file.delete()) {
                log.info("文件删除成功：{}", filePath);
            } else {
                log.error("文件删除失败：{}", filePath);
            }
        }
    }


}
