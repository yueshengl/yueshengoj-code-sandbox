package com.yuesheng.yueshengojcodesandbox.security;

import cn.hutool.core.io.FileUtil;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * @Author: Dai
 * @Date: 2024/12/05 16:37
 * @Description: TestSecurityManager
 * @Version: 1.0
 */
public class TestSecurityManager {

    public static void main(String[] args) {
        System.setSecurityManager(new MySecurityManager());
        FileUtil.writeString("aa","aaa", Charset.defaultCharset());
    }
}
