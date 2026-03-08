package com.pippi.mediatool.common;

import com.pippi.mediatool.common.enums.FileTypeEnum;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * @Author: hong
 * @CreateTime: 2026-02-24
 * @Description: 下载任务-实体类
 * @Version: 1.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class DownloadTask {

    private String taskId;

    private boolean completed;

    private FileTypeEnum fileType;

    private String filePath;

    private LocalDateTime createTime;

    private Double progress;

    public static DownloadTask of(String taskId, FileTypeEnum fileType, String filePath) {
        return new DownloadTask(taskId, false, fileType, filePath, LocalDateTime.now(), 0.00);
    }

}
