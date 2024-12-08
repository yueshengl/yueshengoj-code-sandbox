package com.yuesheng.yueshengojcodesandbox.security;

import java.security.Permission;
/**
 * @Author: Dai
 * @Date: 2024/12/05 16:13
 * @Description: DefaultSecurityManager
 * @Version: 1.0
 */

/**
 * 我的安全管理器
 */
public class MySecurityManager extends SecurityManager {

    //检查所有的权限
    @Override
    public void checkPermission(Permission perm) {
        //super.checkPermission(perm);
    }


    //检测程序是否可执行文件
    @Override
    public void checkExec(String cmd) {
        throw new SecurityException("checkExec 权限异常:" + cmd);
    }

    //检测程序是否可读文件
    @Override
    public void checkRead(String file, Object context) {
        System.out.println(file);
        if (file.contains("F:\\IDEA\\IdeaProjects\\yueshengoj-code-sandbox")) {
            return;
        }
        //throw new SecurityException("checkRead 权限异常:" + file);
    }

    //检测程序是否写文件
    @Override
    public void checkWrite(String file) {
        //throw new SecurityException("checkWrite 权限异常:" + file);
    }

    //检测程序是否允许删除文件
    @Override
    public void checkDelete(String file) {
        //throw new SecurityException("checkDelete 权限异常:" + file);
    }

    //检测程序是否允许连接网络
    @Override
    public void checkConnect(String host, int port) {
        //throw new SecurityException("checkConnect 权限异常:" + host + ":" + port);
    }
}