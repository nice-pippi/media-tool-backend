package com.pippi.mediatool.mvc.co;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * @Author: hong
 * @CreateTime: 2026-02-24
 * @Description: 任务参数-实体
 * @Version: 1.0
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class TaskCO {

    private String taskId;

    private String url;

}
