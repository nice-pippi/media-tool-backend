package com.pippi.mediatool.common.utils;

import com.pippi.mediatool.application.exception.BusinessException;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
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

    /**
     * 复制文件
     *
     * @param sourcePath 源文件路径
     * @param targetPath 目标文件路径
     */
    public static void copyFile(String sourcePath, String targetPath) {
        if (StringUtils.isEmpty(sourcePath) || StringUtils.isEmpty(targetPath)) {
            throw BusinessException.of("文件路径不能为空");
        }

        File sourceFile = new File(sourcePath);
        if (!sourceFile.exists()) {
            throw BusinessException.of("源文件不存在：" + sourcePath);
        }

        try (FileInputStream fis = new FileInputStream(sourceFile);
             FileOutputStream fos = new FileOutputStream(targetPath)) {
            
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = fis.read(buffer)) != -1) {
                fos.write(buffer, 0, bytesRead);
            }
            fos.flush();
            log.info("文件复制成功：{} -> {}", sourcePath, targetPath);
        } catch (IOException e) {
            log.error("文件复制失败：{} -> {}", sourcePath, targetPath, e);
            throw BusinessException.of("文件复制失败：" + e.getMessage());
        }
    }

    /**
     * 获取文件大小（字节）
     *
     * @param filePath 文件路径
     * @return 文件大小，如果文件不存在返回-1
     */
    public static long getFileSize(String filePath) {
        if (StringUtils.isEmpty(filePath)) {
            return -1;
        }

        File file = new File(filePath);
        if (!file.exists()) {
            return -1;
        }

        return file.length();
    }

    /**
     * 格式化文件大小
     *
     * @param sizeInBytes 文件大小（字节）
     * @return 格式化后的字符串
     */
    public static String formatFileSize(long sizeInBytes) {
        if (sizeInBytes < 0) {
            return "未知";
        }

        double kb = sizeInBytes / 1024.0;
        if (kb < 1024) {
            return String.format("%.2f KB", kb);
        }

        double mb = kb / 1024.0;
        if (mb < 1024) {
            return String.format("%.2f MB", mb);
        }

        double gb = mb / 1024.0;
        return String.format("%.2f GB", gb);
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
