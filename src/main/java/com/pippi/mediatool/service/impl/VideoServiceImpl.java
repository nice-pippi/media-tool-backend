package com.pippi.mediatool.service.impl;

import com.pippi.mediatool.application.exception.BusinessException;
import com.pippi.mediatool.common.DownloadTask;
import com.pippi.mediatool.common.constants.FilePathConstant;
import com.pippi.mediatool.common.enums.FileTypeEnum;
import com.pippi.mediatool.common.manager.TaskManager;
import com.pippi.mediatool.common.utils.FileUtil;
import com.pippi.mediatool.mvc.co.TaskCO;
import com.pippi.mediatool.service.VideoService;
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
        String url = co.getUrl();
        Boolean needCompress = co.getNeedCompress();

        // 输出文件路径
        FileUtil.makeDir(FilePathConstant.VIDEO_TEMP_PATH);
        String outputFilePath = FilePathConstant.VIDEO_TEMP_PATH + UUID.randomUUID() + ".mp4";

        // 创建任务对象
        String taskId = co.getTaskId();
        DownloadTask task = DownloadTask.of(taskId, FileTypeEnum.VIDEO, outputFilePath);
        taskManager.addTask(taskId, task);

        try {
            // 获取视频源信息
            FFmpegProbeResult in = ffprobe.probe(url);

            // 根据 needCompress 选择构建不同的 builder
            FFmpegBuilder builder;
            if (needCompress != null && needCompress) {
                builder = buildCompressBuilder(url, outputFilePath);
            } else {
                builder = buildCopyBuilder(url, outputFilePath);
            }

            // 创建FFmpeg执行器
            FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);

            // 创建任务并设置进度监听器
            FFmpegJob job = executor.createJob(builder, new ProgressListener() {
                final double duration_ns = in.getFormat().duration * TimeUnit.SECONDS.toNanos(1);

                @Override
                public void progress(Progress progress) {
                    if (progress.out_time_ns >= 0 && duration_ns > 0) {
                        double percentage = (progress.out_time_ns / duration_ns) * 100;
                        percentage = Math.min(percentage, 100);
                        String percentageStr = String.format("%.2f", percentage);
                        taskManager.updateProgress(taskId, percentage);
                        WebSocketServer.sendProgress(taskId, percentageStr);
                        log.info("任务id：{}，处理进度: {}%", taskId, percentageStr);
                    }
                }
            });

            // 异步执行
            CompletableFuture.runAsync(() -> {
                try {
                    job.run();
                    task.setCompleted(true);
                    if (!task.getProgress().equals(100.00)) {
                        task.setProgress(100.00);
                        WebSocketServer.sendProgress(taskId, "100");
                        log.info("任务id：{}，处理进度: 100%", taskId);
                    }
                    log.info("任务id：{}，处理完成", taskId);
                } catch (Exception e) {
                    FileUtil.deleteFile(outputFilePath);
                    log.error("任务id：{}，处理异常：{}", taskId, e.getMessage());
                }
            });

        } catch (IOException e) {
            FileUtil.deleteFile(outputFilePath);
            log.error("处理异常：{}", e.getMessage());
            throw BusinessException.of("处理异常");
        }
    }

    @Override
    public String simpleDownload(String url, Boolean compress) {
        // 输出文件路径
        FileUtil.makeDir(FilePathConstant.VIDEO_TEMP_PATH);
        String outputFilePath = FilePathConstant.VIDEO_TEMP_PATH + UUID.randomUUID() + ".mp4";

        try {
            // 获取视频源信息
            FFmpegProbeResult in = ffprobe.probe(url);

            // 根据 compress 选择构建不同的 builder
            FFmpegBuilder builder;
            if (compress != null && compress) {
                builder = buildCompressBuilder(url, outputFilePath);
            } else {
                builder = buildCopyBuilder(url, outputFilePath);
            }

            // 创建FFmpeg执行器
            FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);

            // 创建任务并设置进度监听器
            FFmpegJob job = executor.createJob(builder, new ProgressListener() {
                final double duration_ns = in.getFormat().duration * TimeUnit.SECONDS.toNanos(1);

                @Override
                public void progress(Progress progress) {
                    if (progress.out_time_ns >= 0 && duration_ns > 0) {
                        double percentage = (progress.out_time_ns / duration_ns) * 100;
                        percentage = Math.min(percentage, 100);
                        String percentageStr = String.format("%.2f", percentage);
                        log.info("处理进度: {}%", percentageStr);
                    }
                }
            });

            // 异步执行
            CompletableFuture.runAsync(() -> {
                try {
                    job.run();
                    log.info("处理完成，文件保存在：{}", outputFilePath);
                } catch (Exception e) {
                    FileUtil.deleteFile(outputFilePath);
                    log.error("处理异常：{}", e.getMessage());
                }
            });

            return transferWinPath(outputFilePath);

        } catch (IOException e) {
            FileUtil.deleteFile(outputFilePath);
            log.error("处理异常：{}", e.getMessage());
            throw BusinessException.of("处理异常");
        }
    }

    @Override
    public String compress(String filePath) {
        // 输出文件路径
        FileUtil.makeDir(FilePathConstant.VIDEO_TEMP_PATH);
        String outputFilePath = FilePathConstant.VIDEO_TEMP_PATH + UUID.randomUUID() + ".mp4";

        try {
            // 获取视频源信息
            FFmpegProbeResult in = ffprobe.probe(filePath);

            // 构建FFmpeg命令参数
            FFmpegBuilder builder = buildCompressBuilder(filePath, outputFilePath);

            // 创建FFmpeg执行器
            FFmpegExecutor executor = new FFmpegExecutor(ffmpeg, ffprobe);

            // 创建压缩任务并设置进度监听器
            FFmpegJob job = executor.createJob(builder, new ProgressListener() {
                final double duration_ns = in.getFormat().duration * TimeUnit.SECONDS.toNanos(1);

                @Override
                public void progress(Progress progress) {
                    if (progress.out_time_ns >= 0 && duration_ns > 0) {
                        double percentage = (progress.out_time_ns / duration_ns) * 100;
                        percentage = Math.min(percentage, 100);
                        String percentageStr = String.format("%.2f", percentage);
                        log.info("视频压缩进度: {}%", percentageStr);
                    }
                }
            });

            // 异步执行压缩任务
            CompletableFuture.runAsync(() -> {
                try {
                    job.run();
                    log.info("视频压缩完成，输入文件：{}，输出文件：{}", filePath, outputFilePath);
                } catch (Exception e) {
                    FileUtil.deleteFile(outputFilePath);
                    log.error("视频压缩异常：{}", e.getMessage());
                }
            });
            return transferWinPath(outputFilePath);
        } catch (IOException e) {
            FileUtil.deleteFile(outputFilePath);
            log.error("视频压缩异常：{}", e.getMessage());
            throw BusinessException.of("视频压缩异常");
        }
    }

    /**
     * 构建直接复制流的 builder（不压缩）
     *
     * @param url            视频URL
     * @param outputFilePath 输出文件路径
     * @return FFmpegBuilder对象
     */
    private FFmpegBuilder buildCopyBuilder(String url, String outputFilePath) {
        // ffmpeg -i url -c copy output.mp4
        return new FFmpegBuilder()
                .setInput(url)
                .addOutput(outputFilePath)
                .setAudioCodec("copy")
                .setVideoCodec("copy")
                .setStrict(FFmpegBuilder.Strict.EXPERIMENTAL)
                .done();
    }

    /**
     * 构建压缩的 builder（H.265 压缩）
     *
     * @param inputFile      输入文件路径
     * @param outputFilePath 输出文件路径
     * @return FFmpegBuilder对象
     */
    private FFmpegBuilder buildCompressBuilder(String inputFile, String outputFilePath) {
        // ffmpeg -i url -c:v hevc_nvenc -cq 23 -preset slow -c:a copy output.mp4
        return new FFmpegBuilder()
                .setInput(inputFile)
                .addOutput(outputFilePath)
                .setVideoCodec("hevc_nvenc")
                .addExtraArgs("-cq", "23")
                .addExtraArgs("-preset", "slow")
                .setAudioCodec("copy")
                .done();
    }

    /*
     * 转换文件路径为Windows格式
     */
    private String transferWinPath(String fileName) {
        return fileName.replace("\\", "/");
    }

}