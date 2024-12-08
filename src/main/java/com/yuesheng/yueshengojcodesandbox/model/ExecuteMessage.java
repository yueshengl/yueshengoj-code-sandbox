package com.yuesheng.yueshengojcodesandbox.model;

/**
 * @Author: Dai
 * @Date: 2024/12/04 21:26
 * @Description: ExecuteMessage
 * @Version: 1.0
 */

import lombok.Data;

/**
 * 进程执行信息
 */
@Data
public class ExecuteMessage {

    private Integer exitValue;
    private String errorMessage;
    private String message;
    private Long time;
    private Long memory;
}
