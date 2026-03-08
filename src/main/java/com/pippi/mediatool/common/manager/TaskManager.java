package com.pippi.mediatool.common.manager;

import com.pippi.mediatool.application.exception.BusinessException;
import com.pippi.mediatool.common.DownloadTask;
import com.pippi.mediatool.common.utils.FileUtil;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @Author: hong
 * @CreateTime: 2026-03-08
 * @Description: 任务管理类
 * @Version: 1.0
 */
@Slf4j
@Component
public class TaskManager {

    // 存储所有任务
    private final ConcurrentHashMap<String, DownloadTask> taskMap = new ConcurrentHashMap<>();

    /**
     * 添加下载任务到任务列表
     *
     * @param taskId 任务 ID，唯一标识符
     * @param task   下载任务对象
     * @throws BusinessException 当任务已存在时抛出业务异常
     */
    public void addTask(String taskId, DownloadTask task) {
        if (StringUtils.isBlank(taskId)) {
            throw BusinessException.of("任务 ID 不能为空");
        }
        if (task == null) {
            throw BusinessException.of("任务对象不能为空");
        }
        if (taskMap.containsKey(taskId)) {
            log.error("任务已存在，任务 ID：{}", taskId);
            throw BusinessException.of("任务已存在");
        }
        taskMap.put(taskId, task);
        log.info("任务添加成功，当前任务数：{}", taskMap.size());
    }

    /**
     * 根据任务 ID 获取下载任务
     *
     * @param taskId 任务 ID，不能为空或空白字符串
     * @return 如果找到则返回对应的 DownloadTask，否则返回 null
     */
    public DownloadTask getTask(String taskId) {
        if (StringUtils.isBlank(taskId)) {
            return null;
        }
        DownloadTask downloadTask = taskMap.get(taskId);
        if (downloadTask == null) {
            log.error("任务不存在，任务 ID：{}", taskId);
            throw BusinessException.of("任务不存在");
        }
        return downloadTask;
    }

    /**
     * 从任务列表中移除指定任务
     *
     * @param taskId 要移除的任务 ID
     */
    public void removeTask(String taskId) {
        if (StringUtils.isBlank(taskId)) {
            return;
        }
        DownloadTask task = taskMap.remove(taskId);
        if (task != null) {
            log.info("任务移除成功，任务 ID：{}，当前任务数：{}", taskId, taskMap.size());
        }
    }

    /**
     * 更新指定任务的信息
     *
     * @param taskId 任务 ID
     * @param task   新的下载任务对象
     */
    public void updateTask(String taskId, DownloadTask task) {
        if (StringUtils.isBlank(taskId)) {
            throw BusinessException.of("任务 ID 不能为空");
        }
        if (!taskMap.containsKey(taskId)) {
            throw BusinessException.of("任务不存在");
        }
        taskMap.put(taskId, task);
    }

    /**
     * 更新任务的下载进度
     *
     * @param taskId   任务 ID
     * @param progress 下载进度值 (0.0 - 100.0)
     */
    public void updateProgress(String taskId, double progress) {
        if (StringUtils.isBlank(taskId)) {
            throw BusinessException.of("任务 ID 不能为空");
        }
        DownloadTask task = taskMap.get(taskId);
        if (task == null) {
            throw BusinessException.of("任务不存在");
        }
        task.setProgress(progress);
    }

    /**
     * 检查指定任务是否已完成
     *
     * @param taskId 任务 ID
     * @return 任务存在且已完成时返回 true，否则返回 false
     */
    public boolean isCompleted(String taskId) {
        if (StringUtils.isBlank(taskId)) {
            return false;
        }
        DownloadTask task = taskMap.get(taskId);
        return task != null && task.isCompleted();
    }

    /**
     * 获取所有下载任务
     *
     * @return 包含所有任务的 Collection 集合
     */
    public Collection<DownloadTask> getAllTasks() {
        return taskMap.values();
    }

    /**
     * 获取当前任务总数
     *
     * @return 任务数量
     */
    public int getTaskCount() {
        return taskMap.size();
    }

    /**
     * 获取未完成的任务数量
     *
     * @return 未完成任务的数量
     */
    public long getPendingTaskCount() {
        return taskMap.values().stream()
                .filter(task -> !task.isCompleted())
                .count();
    }

    /**
     * 获取已完成的任务数量
     *
     * @return 已完成任务的数量
     */
    public long getCompletedTaskCount() {
        return taskMap.values().stream()
                .filter(DownloadTask::isCompleted)
                .count();
    }


    /**
     * 检查指定任务是否存在
     *
     * @param taskId 任务 ID
     * @return 任务存在时返回 true，否则返回 false
     */
    public boolean exists(String taskId) {
        if (StringUtils.isBlank(taskId)) {
            return false;
        }
        return taskMap.containsKey(taskId);
    }

    /**
     * 每小时清理一次过期任务
     * 清理条件：任务已完成且创建时间超过 1 小时
     * 清理操作：删除下载文件并从内存中移除任务记录
     */
    @Scheduled(cron = "0 0 * * * ?")  // 每小时执行一次
    public void cleanupExpiredTasks() {
        log.info("开始清理过期任务");

        LocalDateTime now = LocalDateTime.now();
        int cleanCount = 0;

        Iterator<Map.Entry<String, DownloadTask>> iterator = taskMap.entrySet().iterator();
        while (iterator.hasNext()) {
            Map.Entry<String, DownloadTask> entry = iterator.next();
            DownloadTask task = entry.getValue();

            if (task == null) continue;

            // 只清理已完成的任务
            if (!task.isCompleted()) continue;

            // 任务创建超过1小时就清理
            if (task.getCreateTime() != null) {
                long hours = ChronoUnit.HOURS.between(task.getCreateTime(), now);
                if (hours >= 1) {
                    String taskId = entry.getKey();
                    // 删除下载的文件
                    FileUtil.deleteFile(task.getFilePath());

                    // 从内存中移除任务
                    iterator.remove();
                    cleanCount++;
                    log.info("清理过期任务: {}, 创建时间: {}", taskId, task.getCreateTime());
                }
            }
        }

        if (cleanCount > 0) {
            log.info("清理完成，共清理 {} 个过期任务", cleanCount);
        }
    }
}