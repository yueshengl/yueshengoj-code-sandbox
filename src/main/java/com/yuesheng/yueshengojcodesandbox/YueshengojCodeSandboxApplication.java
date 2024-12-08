package com.yuesheng.yueshengojcodesandbox;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication
public class YueshengojCodeSandboxApplication {

    public static void main(String[] args) {
        SpringApplication.run(YueshengojCodeSandboxApplication.class, args);
        // 初始化 Docker 镜像
        JavaDockerCodeSandbox.initializeDockerImage();
    }

}
