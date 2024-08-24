package com.example.emos.wx.config.shiro;

import org.apache.shiro.mgt.SecurityManager;
import org.apache.shiro.spring.LifecycleBeanPostProcessor;
import org.apache.shiro.spring.security.interceptor.AuthorizationAttributeSourceAdvisor;
import org.apache.shiro.spring.web.ShiroFilterFactoryBean;
import org.apache.shiro.web.mgt.DefaultWebSecurityManager;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.servlet.Filter;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Configuration
//该类目的是将Filter 和 Realm 添加到Shiro框架中
//不然，建立的Filter 和 Realm类 只是单纯javabeam，并不能其作用
public class ShiroConfig {

    @Bean("securityManager")
//    用于封装Realm对象
    public SecurityManager securityManager(OAuth2Realm realm){
        DefaultWebSecurityManager securityManager=new DefaultWebSecurityManager();
//        封装Realm对象
        securityManager.setRealm(realm);
        securityManager.setRememberMeManager(null);
        return securityManager;
    }

    @Bean("shiroFilter")
//    用于封装Filter对象 和 设置 Filter拦截路径
    public ShiroFilterFactoryBean shiroFilter(SecurityManager securityManager,OAuth2Filter filter){
        ShiroFilterFactoryBean shiroFilter=new ShiroFilterFactoryBean();
//        创建factoryBean 必须用到 之前创建的Securitymanager
        shiroFilter.setSecurityManager(securityManager);

//        将自定义的OAuth2Filter类绑定到Shiro框架中
        Map<String , Filter> map=new HashMap<>();
        map.put("oauth2",filter);
        shiroFilter.setFilters(map);

//        设置一下filter类在什么路径下去拦截 HTTP的请求
        Map<String,String> filterMap=new LinkedHashMap<>();
        filterMap.put("/webjars/**", "anon");
        filterMap.put("/druid/**", "anon");
        filterMap.put("/app/**", "anon");
        filterMap.put("/sys/login", "anon");
        filterMap.put("/swagger/**", "anon");
        filterMap.put("/v2/api-docs", "anon");
        filterMap.put("/swagger-ui.html", "anon");
        filterMap.put("/swagger-resources/**", "anon");
        filterMap.put("/captcha.jpg", "anon");
        filterMap.put("/user/register", "anon");
        filterMap.put("/user/login", "anon");
        filterMap.put("/test/**", "anon");
        filterMap.put("/meeting/recieveNotify", "anon");
//       此刻就会调用OAuth2Filter类，毕竟上面已经进行绑定了
        filterMap.put("/**", "oauth2");

        shiroFilter.setFilterChainDefinitionMap(filterMap);


        return shiroFilter;

    }

    @Bean("lifecycleBeanPostProcessor")
//    管理Shiro对象生命周期
    public LifecycleBeanPostProcessor lifecycleBeanPostProcessor(){
        return new LifecycleBeanPostProcessor();
    }

    @Bean
//    AOP切面类；Web方法执行前，被该AOP切面类拦截下来，然后进行验证权限
    public AuthorizationAttributeSourceAdvisor authorizationAttributeSourceAdvisor(SecurityManager securityManager){
        AuthorizationAttributeSourceAdvisor advisor=new AuthorizationAttributeSourceAdvisor();
//        把securitymanager 传进去即可
        advisor.setSecurityManager(securityManager);
        return advisor;
    }
}
