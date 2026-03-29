package com.pippi.mediatool.mvc.controller;

import com.pippi.mediatool.common.R;
import com.pippi.mediatool.mvc.co.TaskCO;
import com.pippi.mediatool.service.VideoService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

/**
 * @Author: hong
 * @CreateTime: 2026-02-10
 * @Description: 视频-控制器
 * @Version: 1.0
 */
@RestController
@RequestMapping("/video")
public class VideoController {
    @Autowired
    private VideoService videoService;

    @PostMapping("/create-download-task")
    public R<Void> createDownloadTask(@RequestBody TaskCO co) {
        videoService.createDownloadTask(co);
        return R.success();
    }

    @GetMapping("/simple-download")
    public R<String> simpleDownload(@RequestParam String url) {
        return R.success(videoService.simpleDownload(url));
    }

    @GetMapping("/compress")
    public R<String> compress(@RequestParam String filePath) {
        return R.success(videoService.compress(filePath));
    }
}
