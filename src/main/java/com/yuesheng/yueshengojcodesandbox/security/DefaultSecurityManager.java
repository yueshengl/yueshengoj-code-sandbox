package com.yuesheng.yueshengojcodesandbox.security;

import java.security.Permission;
/**
 * @Author: Dai
 * @Date: 2024/12/05 16:13
 * @Description: DefaultSecurityManager
 * @Version: 1.0
 */

/**
 * 默认安全管理器
 */
public class DefaultSecurityManager extends SecurityManager {

    //检查所有的权限
    @Override
    public void checkPermission(Permission perm) {
        System.out.println("默认不做任何限制");
        System.out.println(perm);
        //super.checkPermission(perm);
    }
}