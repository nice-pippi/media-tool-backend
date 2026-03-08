package com.pippi.mediatool.mvc.controller;

import com.pippi.mediatool.service.FileService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * @Author: hong
 * @CreateTime: 2026-03-08
 * @Description: 任务-控制器
 * @Version: 1.0
 */
@RestController
@RequestMapping("/file")
public class FileController {

    @Autowired
    private FileService fileService;

    @GetMapping("/download")
    public void download(@RequestParam String taskId, HttpServletResponse httpServletResponse) {
        fileService.download(taskId, httpServletResponse);
    }
}
