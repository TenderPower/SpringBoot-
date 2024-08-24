package com.example.emos.wx.config.shiro;

import org.springframework.stereotype.Component;

@Component
//实现Filter类 与 AOP切面类 信息传递的 媒介类 （meid）
public class ThreadLocalToken {
    private ThreadLocal<String> local=new ThreadLocal<>();

    public void setToken(String token){
        local.set(token);
    }

    public String getToken(){
        return local.get();
    }

    public void clear(){
        local.remove();
    }
}
