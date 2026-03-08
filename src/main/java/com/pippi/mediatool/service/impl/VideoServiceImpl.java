package com.pippi.mediatool.service.impl;

import com.pippi.mediatool.application.exception.BusinessException;
import com.pippi.mediatool.common.DownloadTask;
import com.pippi.mediatool.common.constants.FilePathConstant;
import com.pippi.mediatool.common.enums.FileTypeEnum;
import com.pippi.mediatool.common.manager.TaskManager;
import com.pippi.mediatool.common.utils.FileUtil;
import com.pippi.mediatool.mvc.co.TaskCO;
import com.pippi.mediatool.service.VideoService;
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
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
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

    @Autowired
    private TaskManager taskManager;

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
        taskManager.addTask(taskId, task);

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
                    double percentage = (progress.out_time_ns / duration_ns) * 100;
                    String percentageStr = String.format("%.2f", percentage);
                    taskManager.updateProgress(taskId, percentage);
//                    WebSocketServer.sendProgress(taskId, percentageStr);
                    log.info("视频下载进度: {}%", percentageStr);
                }
            });

            // 异步执行下载任务
            CompletableFuture.runAsync(() -> {
                try {
                    job.run();
                    task.setCompleted(true);
                    // 考虑到下载视频完成后，进度不一定是100%，这里再设置一次
                    if (!task.getProgress().equals(100.00)) {
                        task.setProgress(100.00);
//                        WebSocketServer.sendProgress(taskId, "100");
                        log.info("视频下载进度: 100%");
                    }
                    log.info("视频下载完成，任务id：{}", taskId);
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
            throw BusinessException.of("下载视频异常");
        }
    }

}