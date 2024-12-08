package com.yuesheng.yueshengojcodesandbox;


/**
 * @Author: Dai
 * @Date: 2024/12/04 14:47
 * @Description: CodeSandbox
 * @Version: 1.0
 */

import com.yuesheng.yueshengojcodesandbox.model.ExecuteCodeRequest;
import com.yuesheng.yueshengojcodesandbox.model.ExecuteCodeResponse;

/**
 * 代码沙箱接口定义
 */
public interface CodeSandbox {
    /**
     * 执行代码
     * @param executeCodeRequest
     * @return
     */
    ExecuteCodeResponse executeCode(ExecuteCodeRequest executeCodeRequest);
}
