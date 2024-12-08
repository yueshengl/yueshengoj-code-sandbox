package com.yuesheng.yueshengojcodesandbox.security;

import java.security.Permission;
/**
 * @Author: Dai
 * @Date: 2024/12/05 16:13
 * @Description: DefaultSecurityManager
 * @Version: 1.0
 */

/**
 * 禁用所有权限安全管理器
 */
public class DenySecurityManager extends SecurityManager {

    //检查所有的权限
    @Override
    public void checkPermission(Permission perm) {
        throw new SecurityException("权限异常:"+perm.toString());
    }
}