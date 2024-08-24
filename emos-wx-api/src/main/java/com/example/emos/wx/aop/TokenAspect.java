package com.example.emos.wx.aop;

import com.example.emos.wx.common.util.R;
import com.example.emos.wx.config.shiro.ThreadLocalToken;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Pointcut;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Aspect //声明切面类，所用使用该注解
@Component
public class TokenAspect {
    @Autowired
    private ThreadLocalToken threadLocalToken;

//    设置切点 （我要拦截这个controller package 里面所有java类里面的所有web方法
    @Pointcut("execution(public * com.example.emos.wx.controller.*.*(..))")
    public void aspect(){

    }


//    拦截所有Web方法的返回值
//    TokenAspect先检测ThreadLocalToken中有没有令牌字符串？
//    如果有就把刷新后的令牌写入Web方法返回的R对象里面

//    定义事件，定义为Around事件，表示方法调用之前的参数要拦截，方法返回的结果也拦截
    @Around("aspect()")//参数是上面的切点方法，目的是给切点方法添加通知事件
    public Object around(ProceedingJoinPoint point) throws Throwable{
        R r=(R)point.proceed();//方法执行结果
        String token=threadLocalToken.getToken();
//        如果ThreadLocal中存在Token，说明是更新的Token
        if(token!=null){
//            往响应中放置Token
            r.put("token",token);
            threadLocalToken.clear();
        }
        return r;
    }
}
