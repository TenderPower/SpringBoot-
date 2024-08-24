package com.example.emos.wx.config;

import io.swagger.annotations.ApiOperation;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import springfox.documentation.builders.ApiInfoBuilder;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.service.ApiInfo;
import springfox.documentation.service.ApiKey;
import springfox.documentation.service.AuthorizationScope;
import springfox.documentation.service.SecurityReference;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spi.service.contexts.SecurityContext;
import springfox.documentation.spring.web.plugins.ApiSelectorBuilder;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import javax.print.Doc;
import java.util.ArrayList;
import java.util.List;

@Configuration //注册一个配置类
@EnableSwagger2 //让swagger生效
public class SwaggerConfig {
    @Bean
    public Docket creatRestApi() {
        Docket docket = new Docket(DocumentationType.SWAGGER_2);
//        在swagger页面设置标题信息，项目的描述信息 ect；
        ApiInfoBuilder builder = new ApiInfoBuilder();
//        设置标题
        builder.title("EMOS在线办公系统---在SwaggerConfig类中");
//        设置完成之后，不能直接将builder交给docket对象，需要进行封装
//        封装成APiInfo 对象
        ApiInfo info = builder.build();
//        再进行传入到docket对象
        docket.apiInfo(info);

//        设置那些类那些方法 添加到 swagger界面上
        ApiSelectorBuilder selectorBuilder = docket.select();
//        设置什么包的那些类 添加到 swagger里面
//        默认 ：把所有包下面的所有类添加到swagger里面
        selectorBuilder.paths(PathSelectors.any());
//         限定 某些类必须使用 特定 注解的方法 才会出现在 swagger页面
//         下面代码，只要某个类加入@ApiOperation注解，那么这个类就会加入到swagger页面上
        selectorBuilder.apis(RequestHandlerSelectors.withMethodAnnotation(ApiOperation.class));

//        把selectorBuilder添加到docket里面
        docket = selectorBuilder.build();

//      下面让swagger 支持 JWT 目的是：将生成好的token 添加到 请求头header上
        ApiKey apiKey = new ApiKey("token", "token", "header");
//      之后的操作就是进行各种的封装，才能让swagger记录填写的 字符串（token)
//       才能把令牌字符串token，每次发请求的时候，将其写到请求头里面
//        1)创建 List 对象
        List<ApiKey> apiKeyList = new ArrayList<>();
//        2)添加对象
        apiKeyList.add(apiKey);
//        3)将List 添加到 Docket - 这样，swagger就知道了，你是在请求头head里面的token参数 提交 令牌字符串
        docket.securitySchemes(apiKeyList);
//
//        接下来，设置令牌的作用域 为 全局
//        1)封装作用域
        AuthorizationScope scope = new AuthorizationScope("global", "accessEverything");
//        2)把认证作用域对象 放到 数组里面
        AuthorizationScope[] scopes = {scope};
//        3)将数组 封装到  SecurityReference
        SecurityReference reference = new SecurityReference("token", scopes);
//        4)将SecurityReference 封装到 ArrayList
        List refList = new ArrayList();
        refList.add(reference);
//        5)将ArrayList 封装成 SecurityContext
        SecurityContext context = SecurityContext.builder().securityReferences(refList).build();
//        6)将SecurityContext 封装到 List
        List cxList = new ArrayList();
        cxList.add(context);
//        7)最后交给docket
        docket.securityContexts(cxList);

        return docket;
    }
}
