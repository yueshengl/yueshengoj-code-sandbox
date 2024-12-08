package com.yuesheng.yueshengojcodesandbox.model;

/**
 * @Author: Dai
 * @Date: 2024/11/30 22:52
 * @Description: JudgeInfo
 * @Version: 1.0
 */

import lombok.Data;

/**
 * 判题信息
 */
@Data
public class JudgeInfo {
    /**
     * 程序执行信息
     */
    private String message;
    /**
     * 消耗内存
     */
    private Long memory;
    /**
     * 消耗时间
     */
    private Long time;
}
