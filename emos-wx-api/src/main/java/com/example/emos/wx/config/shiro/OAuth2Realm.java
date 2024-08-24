package com.example.emos.wx.config.shiro;

import com.example.emos.wx.db.pojo.TbUser;
import com.example.emos.wx.service.UserService;
import org.apache.shiro.authc.*;
import org.apache.shiro.authz.AuthorizationInfo;
import org.apache.shiro.authz.SimpleAuthorizationInfo;
import org.apache.shiro.realm.AuthorizingRealm;
import org.apache.shiro.subject.PrincipalCollection;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Set;

//实现认证与授权的实现方法 -- 创建AuthorizingRealm类的子类，从而实现认证与授权方法
@Component
public class OAuth2Realm extends AuthorizingRealm {
    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserService userService;

    @Override
//    做认证的时候，我们是让JavaWeb 项目传入封装好的 令牌对象， 不是 令牌字符串
//    该方法是用来判断 你传入的封装好的令牌对象 是不是符合我所定义的令牌封装类的类型
    public boolean supports(AuthenticationToken token) {
        return token instanceof OAuth2Token;
    }

    /**
     * 授权(验证权限时调用)
     */

    @Override
//    当走完了认证方法时，Shiro会自动执行授权方法
    // 这个方法是用来做授权的
    protected AuthorizationInfo doGetAuthorizationInfo(PrincipalCollection collection) {
//        因为认证方法已经把user信息封装了（可以看下方doGetAUthenticationInfo返回值）
//        所以可以通过下面的语句得到用户（user）信息
        TbUser user= (TbUser) collection.getPrimaryPrincipal();
//        获取用户的id
        int userId=user.getId();
//        获取用户的权限 查询用户的权限列表
        Set<String> permsSet=userService.searchUserPermissions(userId);
//        创建授权对象
        SimpleAuthorizationInfo info=new SimpleAuthorizationInfo();
//        把权限列表添加到info对象中
        info.setStringPermissions(permsSet);
        return info;
    }

    /**
     * 认证(验证登录时调用)
     */
    @Override
    // 这个方法是用来认证的
    protected AuthenticationInfo doGetAuthenticationInfo(AuthenticationToken token) throws AuthenticationException {
//        TODO 从令牌中获取userId
//        将token转换为String对象
        String accessToken=(String)token.getPrincipal();
//        解析token获取用户id
        int userId=jwtUtil.getUserId(accessToken);
//        查询用户
        TbUser user=userService.searchById(userId);
//        如果当前用户token有效，但已经离职了
        if(user==null){
            throw new LockedAccountException("账号已被锁定,请联系管理员");
        }
//        TODO 往info对象中添加用户信息，Token字符串
        SimpleAuthenticationInfo info=new SimpleAuthenticationInfo(user,accessToken,getName());
        return info;
    }
}
