package com.pippi.mediatool.service;

import com.pippi.mediatool.mvc.co.TaskCO;

import java.util.List;

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

    /**
     * 简单下载
     *
     * @param url          视频地址
     * @param compress 是否需要压缩
     * @return 下载后的文件路径
     */
    String simpleDownload(String url, Boolean compress);

    /**
     * 压缩视频
     *
     * @param filePath 视频文件路径
     * @return 压缩后的文件路径
     */
    String compress(String filePath);

    /**
     * 批量压缩视频文件（按顺序处理）
     *
     * @param filePaths 视频文件路径列表
     */
    void batchCompress(List<String> filePaths);
}
