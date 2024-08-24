package com.example.emos.wx;

import cn.hutool.core.util.StrUtil;
import com.example.emos.wx.config.SystemConstants;
import com.example.emos.wx.db.dao.SysConfigDao;
import com.example.emos.wx.db.pojo.SysConfig;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;
import org.springframework.scheduling.annotation.EnableAsync;

import javax.annotation.PostConstruct;
import java.io.File;
import java.lang.reflect.Field;
import java.util.List;

@SpringBootApplication
@ServletComponentScan //不加该注解的话，刚才创建的Filter不会生效
@Slf4j
@EnableAsync //开启异步多线程
public class EmosWxApiApplication {

    @Autowired
    private SysConfigDao sysConfigDao;
    @Autowired
    private SystemConstants systemConstants;

    @Value("${emos.image-folder}")
    private String imageFolder;

    public static void main(String[] args) {
        SpringApplication.run(EmosWxApiApplication.class, args);
    }

//    在SpringBoot项目启动的时候，就去数据库读取考勤模块的常量信息
//    然后缓存成Java对象，全局都可以使用
//    创建init()方法， 读取常量数据并进行缓存
    @PostConstruct //在方法上加该注解会在项目启动的时候执行该方法
    public void init(){
//        调用持久层获取常量信息
        List<SysConfig> list = sysConfigDao.selectAllParam();
//        对list的每个元素进行存储
        list.forEach(one->{
            String key = one.getParamKey();
//            因为数据库中的key是有下划线的，要将其转为驼峰命名规则
//            便于存放到SystemConstant类设置的变量里
            key = StrUtil.toCamelCase(key);
            String value = one.getParamValue();
            try {
//                在运行时获取当前对象所属类中名为 key 的字段的信息
                Field field = systemConstants.getClass().getDeclaredField(key);
//                将数据库中读取的信息 缓存成Java对象
                field.set(systemConstants, value);
            }catch (Exception e) {
                log.error("启动初始化时，init执行异常",e);
            }
        });

//        创建暂存人脸图片的文件
        new File(imageFolder).mkdirs();
    }

}
