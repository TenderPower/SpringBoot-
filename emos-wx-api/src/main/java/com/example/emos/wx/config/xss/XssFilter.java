package com.example.emos.wx.config.xss;

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;

//该类作用：
//把拦截下来的请求 封装成 Wrapper对象（XSSHttpServietRequestWrapper)
//这样 才能对请求的数据 进行转义操作


//@WebFilter 注解定义出来的过滤器，
// 他的优先级比 SpringMVC 中注册的 Filter 优先级更高，所以 XSSFilter 早于 SpringMVC 执行
@WebFilter(urlPatterns = "/*")//设置拦截请求的路径为/*
public class XssFilter implements Filter {
    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
//        先对请求 进行一次 转型 ，方便传入到wrapper中
        HttpServletRequest request= (HttpServletRequest) servletRequest;
//          把自己创建的Wrapper类 创建一下
        XssHttpServletRequestWrapper wrapper=new XssHttpServletRequestWrapper(request);
//        让过滤器 继续往后执行
        filterChain.doFilter(wrapper,servletResponse);
    }

    @Override
    public void destroy() {

    }
}
