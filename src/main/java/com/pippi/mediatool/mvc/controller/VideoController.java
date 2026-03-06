package com.pippi.mediatool.mvc.controller;

import com.pippi.mediatool.common.R;
import com.pippi.mediatool.mvc.co.TaskCO;
import com.pippi.mediatool.service.impl.VideoServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

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
    private VideoServiceImpl videoService;

    @PostMapping("/create-download-task")
    public R<Void> createDownloadTask(@RequestBody TaskCO co) {
        videoService.createDownloadTask(co);
        return R.success();
    }

}
