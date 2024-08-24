package com.example.emos.wx.config.shiro;

import cn.hutool.core.util.StrUtil;
import com.auth0.jwt.exceptions.JWTDecodeException;
import com.auth0.jwt.exceptions.TokenExpiredException;
import org.apache.http.HttpStatus;
import org.apache.shiro.authc.AuthenticationException;
import org.apache.shiro.authc.AuthenticationToken;
import org.apache.shiro.web.filter.authc.AuthenticatingFilter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.annotation.RequestMethod;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.TimeUnit;

@Component
@Scope("prototype") //容器获取Bean时，都会创建一个新的实例
//因为OAuth2Filter是在SpringMVC中注册的Filter，所以它晚于Servlet过滤器的执行
//该Filter类的作用： 验证Http的请求头是否存在token，以及token是否过期
//如果该HTTP请求通过了Filter类，就会自动进入OAuth2Realm类，进行下一步的认证操作和授权操作
public class OAuth2Filter extends AuthenticatingFilter {
    @Autowired
    private ThreadLocalToken threadLocalToken;

    @Value("${emos.jwt.cache-expire}")
    private int cacheExpire;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private RedisTemplate redisTemplate;

    @Override
//    返回抽象的令牌对象
//    该方法 是从请求里面获取令牌字符串，然后将令牌字符串封装成令牌对象，该令牌对象将来会交给shiro框架去使用
    protected AuthenticationToken createToken(ServletRequest request, ServletResponse response) throws Exception {
        HttpServletRequest req = (HttpServletRequest) request;
//        获取请求token
        String token = getRequestToken(req);
//        判断token是否为空
        if (StrUtil.isBlank(token)) {
            return null;
        }
//        将token进行封装
        return new OAuth2Token(token);
    }

    @Override
//    该方法是为了判断，那些请求需要被shiro处理，那些请求不需要被shiro处理
    protected boolean isAccessAllowed(ServletRequest request, ServletResponse response, Object mappedValue) {
//        将ServletRequest类型 强制转为 HttpServletRequest类型
        HttpServletRequest req = (HttpServletRequest) request;
//        判读请求是不是Option请求
        if (req.getMethod().equals(RequestMethod.OPTIONS.name())) {
            return true;
        }
        return false;
    }

    @Override
//    这个方法在 当isAccessAllowed方法确定某请求需要被shiro处理时 才执行
    protected boolean onAccessDenied(ServletRequest request, ServletResponse response) throws Exception {
//        类型转换，把不是http类型的数据进行转换
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
//        在响应里面 设置 响应头
        resp.setContentType("text/html");
//        响应字符集
        resp.setCharacterEncoding("UTF-8");
//        在响应头里面 设置 跨域的参数 （在开发前后端分离的项目时，后端项目允许跨域请求 ，就要在响应头里面加入这两个参数）
        resp.setHeader("Access-Control-Allow-Credentials", "true");
        resp.setHeader("Access-Control-Allow-Origin", req.getHeader("Origin"));

        threadLocalToken.clear();
//        返回令牌字符串
        String token = getRequestToken(req);

        if (StrUtil.isBlank(token)) {
            resp.setStatus(HttpStatus.SC_UNAUTHORIZED);
            resp.getWriter().print("无效的令牌");
            return false;
        }
        try {
            jwtUtil.verifierToken(token);
        } catch (TokenExpiredException e) {
//            捕获 令牌过期的异常
//            判断Redis缓冲中 是否缓存了令牌数据
            if (redisTemplate.hasKey(token)) {
//                如果有的话，代表Client的令牌数据过期了， 但是，服务端的令牌没有过期
//                1）先删掉老令牌
                redisTemplate.delete(token);

//                2） 进行令牌的刷新，生成新的令牌
                int userId = jwtUtil.getUserId(token);
//                生成新的令牌
                token = jwtUtil.createToken(userId);
//                将新生成的令牌，保存在Redis缓冲中
                redisTemplate.opsForValue().set(token, userId + "", cacheExpire, TimeUnit.DAYS);
//                保持到媒介类中
                threadLocalToken.setToken(token);
            } else {
//                Client端令牌过期了，服务器的令牌也过期了
//                需要用户重新登录
                resp.setStatus(HttpStatus.SC_UNAUTHORIZED);
                resp.getWriter().print("令牌已过期");
                return false;
            }
        } catch (Exception e) {

            resp.setStatus(HttpStatus.SC_UNAUTHORIZED);
            resp.getWriter().print("无效的令牌");
            return false;
        }
//        让shiro间接执行 (重新生成令牌后，相当于你不用自己重新登录了，机器帮你登录了）
        boolean bool = executeLogin(request, response);
        return bool;
    }

    @Override
//    当登录（认证）失败时，进行响应
    protected boolean onLoginFailure(AuthenticationToken token, AuthenticationException e, ServletRequest request, ServletResponse response) {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        resp.setContentType("text/html");
        resp.setCharacterEncoding("UTF-8");
        resp.setHeader("Access-Control-Allow-Credentials", "true");
        resp.setHeader("Access-Control-Allow-Origin", req.getHeader("Origin"));
        resp.setStatus(HttpStatus.SC_UNAUTHORIZED);
        try {
            resp.getWriter().print(e.getMessage());
        } catch (Exception exception) {

        }

        return false;
    }

    @Override
    public void doFilterInternal(ServletRequest request, ServletResponse response, FilterChain chain) throws ServletException, IOException {
        HttpServletRequest req = (HttpServletRequest) request;
        HttpServletResponse resp = (HttpServletResponse) response;
        resp.setContentType("text/html");
        resp.setCharacterEncoding("UTF-8");

        resp.setHeader("Access-Control-Allow-Credentials", "true");
        resp.setHeader("Access-Control-Allow-Origin", req.getHeader("Origin"));
        super.doFilterInternal(request, response, chain);

    }

    private String getRequestToken(HttpServletRequest request) {
        String token = request.getHeader("token");
        if (StrUtil.isBlank(token)) {
            token = request.getParameter("token");
        }
        return token;
    }
}
