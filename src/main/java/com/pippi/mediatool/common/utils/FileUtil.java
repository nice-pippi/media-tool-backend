package com.pippi.mediatool.common.utils;

import com.pippi.mediatool.application.exception.BusinessException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStream;

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


    public static void outputFile(String filePath, HttpServletResponse response) {
        File file = new File(filePath);

        // 检查文件是否存在
        if (!file.exists()) {
            throw BusinessException.of("文件不存在: " + filePath);
        }

        // 设置响应头
        response.setContentType("application/octet-stream");
        response.setHeader("Content-Disposition", "attachment; filename=\"" + file.getName() + "\"");
        response.setContentLengthLong(file.length());

        // 读取文件并输出
        try (FileInputStream fis = new FileInputStream(file);
             OutputStream os = response.getOutputStream()) {

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }
            os.flush();
        } catch (IOException e) {
            log.error("文件下载失败：{}", filePath, e);
            throw BusinessException.of("文件下载失败: " + e.getMessage());
        }
    }
}
