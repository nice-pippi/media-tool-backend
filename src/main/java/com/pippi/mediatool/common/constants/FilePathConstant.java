package com.pippi.mediatool.common.constants;

import java.io.File;

/**
 * @Author: hong
 * @CreateTime: 2026-02-24
 * @Description: 临时文件路径常量
 * @Version: 1.0
 */
public interface FilePathConstant {

    String VIDEO_TEMP_PATH = System.getProperty("user.dir") + File.separator +
            "temp" + File.separator + "video" + File.separator;
}
