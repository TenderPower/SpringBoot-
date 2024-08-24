package com.example.emos.wx.config;

import com.rabbitmq.client.ConnectionFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

//要想使用RabbitMQ 首先要构造链接，这样才能连接到RabbitMQ，才能使用他
//连接 RabbitMQ 需要用到 ConnectionFactory
@Configuration
public class RabbitMQConfig {
    //连接 RabbitMQ 需要用到 ConnectionFactory
    @Bean
    public ConnectionFactory getFactory(){
        ConnectionFactory factory = new ConnectionFactory();
//        去连接远程服务器 （Linux主机的IP地址）
        factory.setHost("192.168.31.99");
        factory.setPort(5672);//RabbitMQ端口号
        factory.setPassword("password");
        factory.setUsername("admin");
        return factory;
    }

}
