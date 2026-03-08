package com.pippi.mediatool.service.impl;

import com.pippi.mediatool.application.exception.BusinessException;
import com.pippi.mediatool.common.DownloadTask;
import com.pippi.mediatool.common.manager.TaskManager;
import com.pippi.mediatool.common.utils.FileUtil;
import com.pippi.mediatool.service.FileService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * @Author: hong
 * @CreateTime: 2026-03-08
 * @Description: 件业务层-实现
 * @Version: 1.0
 */
@Slf4j
@Service
public class FileServiceImpl implements FileService {

    @Autowired
    private TaskManager taskManager;

    @Override
    public void download(String taskId, HttpServletResponse httpServletResponse) {
        DownloadTask downloadTask = taskManager.getTask(taskId);
        if (!downloadTask.isCompleted()) {
            throw BusinessException.of("任务未完成");
        }

        String filePath = downloadTask.getFilePath();

        // 输出到httpServletResponse
        FileUtil.outputFile(filePath, httpServletResponse);

        // 下载完成后删除任务和文件
        taskManager.removeTask(taskId);
        FileUtil.deleteFile(filePath);
    }
}
