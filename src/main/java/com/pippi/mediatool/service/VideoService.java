package com.pippi.mediatool.service;

import com.pippi.mediatool.mvc.co.TaskCO;

/**
 * @Author: hong
 * @CreateTime: 2026-02-21
 * @Description: 视频业务层
 * @Version: 1.0
 */
public interface VideoService {

    /**
     * 创建下载任务
     *
     * @param co 任务参数
     */
    void createDownloadTask(TaskCO co);

}
