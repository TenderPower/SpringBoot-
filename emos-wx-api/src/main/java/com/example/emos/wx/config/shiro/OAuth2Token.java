package com.example.emos.wx.config.shiro;

import org.apache.shiro.authc.AuthenticationToken;

//把字符串令牌 封装成 （认证）对象，
//因为Shiro框架的认证 需要 用到（认证）对象
//所以将 字符串令牌 做简单的封装

//把Token字符串令牌 封装成 （认证）对象 即OBject
public class OAuth2Token implements AuthenticationToken {
    private String token;

    public OAuth2Token(String token) {
        this.token = token;
    }

    @Override
    public Object getPrincipal() {
        return token;
    }

    @Override
    public Object getCredentials() {
        return token;
    }
}
