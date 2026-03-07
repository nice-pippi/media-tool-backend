package com.pippi.mediatool.service.impl;

import com.pippi.mediatool.common.DownloadTask;
import com.pippi.mediatool.common.constants.FilePathConstant;
import com.pippi.mediatool.common.enums.FileTypeEnum;
import com.pippi.mediatool.application.exception.BusinessException;
import com.pippi.mediatool.mvc.co.TaskCO;
import com.pippi.mediatool.service.VideoService;
import com.pippi.mediatool.common.utils.FileUtil;
import com.pippi.mediatool.websocket.WebSocketServer;
import lombok.extern.slf4j.Slf4j;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import net.bramp.ffmpeg.job.FFmpegJob;
import net.bramp.ffmpeg.probe.FFmpegProbeResult;
import net.bramp.ffmpeg.progress.Progress;
import net.bramp.ffmpeg.progress.ProgressListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Iterator;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @Author: hong
 * @CreateTime: 2026-02-21
 * @Description: 视频业务层-实现
 * @Version: 1.0
 */
@Slf4j
@Service
public class VideoServiceImpl implements VideoService {

    @Autowired
    private FFmpeg ffmpeg;

    @Autowired
    private FFprobe ffprobe;

    // taskId -> DownloadTask
    private static final ConcurrentHashMap<String, DownloadTask> TASK_MAP = new ConcurrentHashMap<>();

    @Override
    public void createDownloadTask(TaskCO co) {
        // 视频地址
        String url = co.getUrl();

        // 输出文件路径
        FileUtil.makeDir(FilePathConstant.VIDEO_TEMP_PATH);
        String fileName = FilePathConstant.VIDEO_TEMP_PATH + UUID.randomUUID() + ".mp4";

        // 创建任务对象
        String taskId = co.getTaskId();
        DownloadTask task = DownloadTask.of(taskId, FileTypeEnum.VIDEO, fileName);
        TASK_MAP.put(taskId, task);

        try {
            // 获取视频源信息
            FFmpegProbeResult in = ffprobe.probe(url);

            // 构建FFmpeg命令参数
            FFmpegBuilder builder = new FFmpegBuilder()
                    .setInput(url)
                    .addOutput(fileName)
                    .setAudioCodec("copy")
                    .setVideoCodec("copy")
                    .done();

            // 创建FFmpeg执行器
            FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);

            // 创建下载任务并设置进度监听器
            FFmpegJob job = executor.createJob(builder, new ProgressListener() {
                // 获取视频总时长（纳秒）
                final double duration_ns = in.getFormat().duration * TimeUnit.SECONDS.toNanos(1);

                @Override
                public void progress(Progress progress) {
                    // 下载进度百分比
                    int percentage = (int) Math.round(progress.out_time_ns / duration_ns * 100);
//                    WebSocketServer.sendProgress(taskId, percentage);
                    log.info("视频下载进度: {}", percentage);
                }
            });

            // 异步执行下载任务
            CompletableFuture.runAsync(() -> {
                try {
                    job.run();
                    task.setCompleted(true);
                    log.info("视频下载完成: {}", taskId);
                } catch (Exception e) {
                    // 删除文件
                    FileUtil.deleteFile(fileName);

                    log.error("下载视频异常：{}", e.getMessage());
                }
            });
        } catch (IOException e) {
            // 删除文件
            FileUtil.deleteFile(fileName);

            log.error("下载视频异常：{}", e.getMessage());
            throw new BusinessException("下载视频异常");
        }
    }


    /**
     * 每小时清理一次过期任务
     */
    @Scheduled(cron = "0 0 * * * ?")  // 每小时执行一次
    public void cleanupExpiredTasks() {
        log.info("开始清理过期任务");

        LocalDateTime now = LocalDateTime.now();
        int cleanCount = 0;

        Iterator<Map.Entry<String, DownloadTask>> iterator = TASK_MAP.entrySet().iterator();
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