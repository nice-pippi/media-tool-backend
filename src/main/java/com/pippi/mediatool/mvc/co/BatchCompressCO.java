package com.pippi.mediatool.mvc.co;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * @Author: hong
 * @CreateTime: 2026-05-01
 * @Description: 批量压缩参数-实体
 * @Version: 1.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class BatchCompressCO {

    /**
     * 视频文件路径列表
     */
    private List<String> filePaths;

    /**
     * 是否删除源文件
     */
    private Boolean deleteSource;

}
