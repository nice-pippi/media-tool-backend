package com.pippi.mediatool.service;

import jakarta.servlet.http.HttpServletResponse;

/**
 * @Author: hong
 * @CreateTime: 2026-03-08
 * @Description: 文件业务层
 * @Version: 1.0
 */
public interface FileService {

    /**
     * 下载文件
     *
     * @param taskId              任务ID
     * @param httpServletResponse 响应
     */
    void download(String taskId, HttpServletResponse httpServletResponse);

}
