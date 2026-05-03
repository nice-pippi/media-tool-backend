package com.pippi.mediatool.service.impl;

import com.pippi.mediatool.application.exception.BusinessException;
import com.pippi.mediatool.common.DownloadTask;
import com.pippi.mediatool.common.constants.FilePathConstant;
import com.pippi.mediatool.common.enums.FileTypeEnum;
import com.pippi.mediatool.common.manager.TaskManager;
import com.pippi.mediatool.common.utils.FileUtil;
import com.pippi.mediatool.mvc.co.BatchCompressCO;
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
import net.bramp.ffmpeg.probe.FFmpegStream;
import net.bramp.ffmpeg.progress.Progress;
import net.bramp.ffmpeg.progress.ProgressListener;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
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

    private final Integer DEFAULT_CRF = 23;

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

                    // 检查压缩后文件大小
                    long sourceSize = FileUtil.getFileSize(filePath);
                    long compressedSize = FileUtil.getFileSize(outputFilePath);

                    if (sourceSize > 0 && compressedSize > 0) {
                        if (compressedSize >= sourceSize) {
                            log.warn("压缩后文件变大：原文件{}，压缩后{}，删除压缩文件",
                                    FileUtil.formatFileSize(sourceSize),
                                    FileUtil.formatFileSize(compressedSize));
                            FileUtil.deleteFile(outputFilePath);
                            // 复制原文件作为输出
                            FileUtil.copyFile(filePath, outputFilePath);
                        } else {
                            log.info("视频压缩完成，输入文件：{}({})，输出文件：{}({})，减少：{}%",
                                    filePath, FileUtil.formatFileSize(sourceSize),
                                    outputFilePath, FileUtil.formatFileSize(compressedSize),
                                    String.format("%.2f", (1 - (double) compressedSize / sourceSize) * 100));
                        }
                    } else {
                        log.info("视频压缩完成，输入文件：{}，输出文件：{}", filePath, outputFilePath);
                    }
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

    @Override
    public void batchCompress(BatchCompressCO co) {
        List<String> filePaths = co.getFilePaths();
        Boolean deleteSource = co.getDeleteSource();

        if (CollectionUtils.isEmpty(filePaths)) {
            throw BusinessException.of("文件路径列表不能为空");
        }

        // 记录成功压缩的源文件路径，用于最后统一删除
        List<String> successFiles = new ArrayList<>();

        // 按顺序处理每个文件
        for (String filePath : filePaths) {
            String outputFilePath = null;
            try {
                log.info("开始压缩文件：{}", filePath);

                // 输出文件路径
                FileUtil.makeDir(FilePathConstant.VIDEO_TEMP_PATH);
                outputFilePath = FilePathConstant.VIDEO_TEMP_PATH + UUID.randomUUID() + ".mp4";

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
                            log.info("视频压缩进度：{}%", percentageStr);
                        }
                    }
                });

                // 同步执行压缩任务（阻塞等待完成）
                job.run();

                // 检查压缩后文件大小
                long sourceSize = FileUtil.getFileSize(filePath);
                long compressedSize = FileUtil.getFileSize(outputFilePath);

                if (sourceSize > 0 && compressedSize > 0) {
                    if (compressedSize >= sourceSize) {
                        log.warn("压缩后文件变大：原文件{}，压缩后{}，删除压缩文件",
                                FileUtil.formatFileSize(sourceSize),
                                FileUtil.formatFileSize(compressedSize));
                        FileUtil.deleteFile(outputFilePath);
                        // 复制原文件作为输出
                        FileUtil.copyFile(filePath, outputFilePath);
                    } else {
                        log.info("视频压缩完成，输入文件：{}({})，输出文件：{}({})，减少：{}%",
                                filePath, FileUtil.formatFileSize(sourceSize),
                                outputFilePath, FileUtil.formatFileSize(compressedSize),
                                String.format("%.2f", (1 - (double) compressedSize / sourceSize) * 100));
                    }
                } else {
                    log.info("视频压缩完成，输入文件：{}，输出文件：{}", filePath, outputFilePath);
                }

                // 记录成功压缩的文件
                successFiles.add(filePath);
            } catch (Exception e) {
                if (outputFilePath != null) {
                    FileUtil.deleteFile(outputFilePath);
                }
                log.error("视频压缩异常：{}", e.getMessage());
            }
        }

        // 所有文件处理完成后，统一删除成功的源文件
        if (deleteSource != null && deleteSource && !CollectionUtils.isEmpty(successFiles)) {
            log.info("开始删除源文件，共 {} 个", successFiles.size());
            for (String filePath : successFiles) {
                try {
                    FileUtil.deleteFile(filePath);
                    log.info("已删除源文件：{}", filePath);
                } catch (Exception e) {
                    log.error("删除源文件失败：{}，错误信息：{}", filePath, e.getMessage());
                }
            }
            log.info("源文件删除完成");
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
     * 构建压缩的 builder（H.265 压缩，支持动态CRF）
     *
     * @param inputFile      输入文件路径
     * @param outputFilePath 输出文件路径
     * @return FFmpegBuilder对象，如果不需要压缩则返回null
     */
    private FFmpegBuilder buildCompressBuilder(String inputFile, String outputFilePath) {
        try {
            // 获取视频源信息
            FFmpegProbeResult probeResult = ffprobe.probe(inputFile);

            // 计算动态CRF值
            int crf = calculateDynamicCRF(probeResult);
            log.info("使用动态CRF值：{} 压缩视频：{}", crf, inputFile);

            // ffmpeg -i input.mp4 -c:v libx265 -crf {crf} -c:a copy output.mp4
            return new FFmpegBuilder()
                    .setInput(inputFile)
                    .addOutput(outputFilePath)
                    .setVideoCodec("libx265")
                    .addExtraArgs("-crf", String.valueOf(crf))
                    .setAudioCodec("copy")
                    .setStrict(FFmpegBuilder.Strict.EXPERIMENTAL)
                    .done();
        } catch (IOException e) {
            log.error("计算动态CRF异常：{}", e.getMessage());
            throw BusinessException.of("计算动态CRF异常: " + e.getMessage());
        }
    }

    /**
     * 根据视频信息动态计算CRF值
     *
     * @param probeResult 视频探测结果
     * @return CRF值（18-28范围）
     */
    private int calculateDynamicCRF(FFmpegProbeResult probeResult) {
        // 查找视频流
        FFmpegStream videoStream = probeResult.getStreams().stream()
                .filter(stream -> stream.codec_type == FFmpegStream.CodecType.VIDEO)
                .findFirst()
                .orElse(null);

        if (videoStream == null) {
            return DEFAULT_CRF; // 默认值
        }

        // 获取码率（bps）
        double bitRate = videoStream.bit_rate > 0 ? videoStream.bit_rate :
                (probeResult.getFormat() != null ? probeResult.getFormat().bit_rate : 0);

        if (bitRate <= 0) {
            return DEFAULT_CRF; // 无法获取码率时使用默认值
        }

        double bitRateKbps = bitRate / 1000;

        // 根据码率动态调整CRF值（修正版）
        // CRF值越大 = 压缩越强 = 文件越小 = 质量越低
        // CRF值越小 = 压缩越弱 = 文件越大 = 质量越高
        // 策略：高码率视频用较小CRF（强压缩），低码率视频用较大CRF（避免变大）
        int crf;
        if (bitRateKbps > 8000) {
            crf = DEFAULT_CRF; // 高码率视频，适度压缩
        } else if (bitRateKbps > 4000) {
            crf = 24; // 中高码率
        } else if (bitRateKbps > 2000) {
            crf = 25; // 中等码率
        } else if (bitRateKbps > 1500) {
            crf = 26; // 中低码率，较强压缩
        } else {
            crf = 28; // 低码率，最强压缩避免文件变大
        }
        return crf;
    }

    /*
     * 转换文件路径为Windows格式
     */
    private String transferWinPath(String fileName) {
        return fileName.replace("\\", "/");
    }

    @Override
    public void dirCompress(String dirPath, Boolean deleteSource) {
        if (StringUtils.isEmpty(dirPath)) {
            throw BusinessException.of("目录路径不能为空");
        }

        File directory = new File(dirPath);
        if (!directory.exists() || !directory.isDirectory()) {
            throw BusinessException.of("目录不存在或不是有效目录：" + dirPath);
        }

        // 获取目录下所有视频文件
        File[] videoFiles = directory.listFiles((dir, name) -> {
            String lowerName = name.toLowerCase();
            return lowerName.endsWith(".mp4") || lowerName.endsWith(".avi") ||
                    lowerName.endsWith(".mov") || lowerName.endsWith(".mkv") ||
                    lowerName.endsWith(".flv") || lowerName.endsWith(".wmv");
        });

        if (videoFiles == null || videoFiles.length == 0) {
            log.warn("目录下没有找到视频文件：{}", dirPath);
            return;
        }

        log.info("找到 {} 个视频文件，开始批量压缩", videoFiles.length);

        // 记录成功压缩的文件
        List<String> successFiles = new ArrayList<>();

        // 按顺序处理每个文件
        for (File videoFile : videoFiles) {
            String filePath = videoFile.getAbsolutePath();
            String outputFilePath = null;
            try {
                log.info("开始压缩文件：{}", filePath);

                // 输出文件路径
                FileUtil.makeDir(FilePathConstant.VIDEO_TEMP_PATH);
                outputFilePath = FilePathConstant.VIDEO_TEMP_PATH + UUID.randomUUID() + ".mp4";

                // 获取视频源信息
                FFmpegProbeResult in = ffprobe.probe(filePath);

                // 构建FFmpeg命令参数
                FFmpegBuilder builder = buildCompressBuilder(filePath, outputFilePath);

                // 如果不需要压缩（返回null），跳过该文件
                if (builder == null) {
                    log.info("视频无需压缩，跳过：{}", filePath);
                    FileUtil.copyFile(filePath, outputFilePath);
                    successFiles.add(filePath);
                    continue;
                }

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
                            log.info("视频压缩进度：{}%", percentageStr);
                        }
                    }
                });

                // 同步执行压缩任务（阻塞等待完成）
                job.run();

                // 检查压缩后文件大小
                long sourceSize = FileUtil.getFileSize(filePath);
                long compressedSize = FileUtil.getFileSize(outputFilePath);

                if (sourceSize > 0 && compressedSize > 0) {
                    if (compressedSize >= sourceSize) {
                        log.warn("压缩后文件变大：原文件{}，压缩后{}，删除压缩文件",
                                FileUtil.formatFileSize(sourceSize),
                                FileUtil.formatFileSize(compressedSize));
                        FileUtil.deleteFile(outputFilePath);
                        // 复制原文件作为输出
                        FileUtil.copyFile(filePath, outputFilePath);
                    } else {
                        log.info("视频压缩完成，输入文件：{}({})，输出文件：{}({})，减少：{}%",
                                filePath, FileUtil.formatFileSize(sourceSize),
                                outputFilePath, FileUtil.formatFileSize(compressedSize),
                                String.format("%.2f", (1 - (double) compressedSize / sourceSize) * 100));
                    }
                } else {
                    log.info("视频压缩完成，输入文件：{}，输出文件：{}", filePath, outputFilePath);
                }

                // 记录成功压缩的文件
                successFiles.add(filePath);
            } catch (Exception e) {
                if (outputFilePath != null) {
                    FileUtil.deleteFile(outputFilePath);
                }
                log.error("视频压缩异常：{}", e.getMessage());
            }
        }

        // 所有文件处理完成后，统一删除成功的源文件
        if (deleteSource != null && deleteSource && !CollectionUtils.isEmpty(successFiles)) {
            log.info("开始删除源文件，共 {} 个", successFiles.size());
            for (String filePath : successFiles) {
                try {
                    FileUtil.deleteFile(filePath);
                    log.info("已删除源文件：{}", filePath);
                } catch (Exception e) {
                    log.error("删除源文件失败：{}，错误信息：{}", filePath, e.getMessage());
                }
            }
            log.info("源文件删除完成");
        }

        log.info("目录压缩完成，共处理 {} 个文件，成功 {} 个", videoFiles.length, successFiles.size());
    }

}