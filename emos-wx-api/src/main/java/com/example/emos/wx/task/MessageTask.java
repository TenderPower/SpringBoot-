package com.example.emos.wx.task;

import com.example.emos.wx.db.pojo.MessageEntity;
import com.example.emos.wx.db.pojo.MessageRefEntity;
import com.example.emos.wx.exception.EmosException;
import com.example.emos.wx.service.MessageService;
import com.rabbitmq.client.*;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

//我们要创建的多线程任务类是用来收发RabbitMQ消息的
//1.JAVA方面使用异步线程去执行RabbitMQ
//2.RabbitMQ在收发消息时时同步执行的
@Component
@Slf4j
public class MessageTask {
    @Autowired
    private ConnectionFactory factory;//连接RabbitMQ

    @Autowired
    private MessageService messageService; //


//    在mq中，每个消息都有自己的名字，这个名字叫topic
//    将MessageENtity 发送到 rabbitmq中的某个topic上
    public void send(String topic, MessageEntity entity) {
//        将数据存放到message集合中
        String id = messageService.insertMessage(entity);
//        接着向rabbitmq发送消息
//        1.向rabbitmq创建连接
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel();
        ) {
//            开始连接队列queue
            channel.queueDeclare(topic, true, false, false, null);
//            2.附属一些其他信息（但该信息要附属到请求头上）
            HashMap map = new HashMap();
            map.put("messageId", id);
//            将hashmap绑定到请求头（AMQP.BasicPRoperties）上
            AMQP.BasicProperties properties = new AMQP.BasicProperties().builder().headers(map).build();
//            3.开始发送消息
            channel.basicPublish("", topic, properties, entity.getMsg().getBytes());//第三个参数：消息的正文
            log.debug("消息发送成功");
        } catch (Exception e) {
            log.error("执行异常", e);
            throw new EmosException("向MQ发送消息失败");
        }
    }

//    构造异步执行的方法
    @Async
    public void sendAsync(String topic, MessageEntity entity) {
        send(topic, entity);
    }

//    从rabbitmq中的某个消息topic队列中 提取出来，即接收消息
    public int receive(String topic) {
//        1.从消息队列中接受消息
//        2.将接受到的消息 保存到 mogobd中的ref集合中的
//        3.保存完成后，向消息队列中发送Ack应答，这样才能代表我成功接受了消息
        int i = 0;//用于累加接收消息的数量
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel();//向rabbitmq创建连接
        ) {
            //            开始连接队列queue
            channel.queueDeclare(topic, true, false, false, null);
            while (true) {
//                接收topic队列的消息（参数2：手动设置ack应答）
                GetResponse response = channel.basicGet(topic, false);
//                判断响应是否为空
                if (response != null) {
//                    从响应中提取绑定的属性数据
                    AMQP.BasicProperties properties = response.getProps();
//                    获取请求头的数据
                    Map<String, Object> map = properties.getHeaders();
//                    从map对象中获取 我们绑定的messageId
                    String messageId = map.get("messageId").toString();
//                    获取消息的正文
                    byte[] body = response.getBody();
//                    将正文的byte数组 转换
                    String message = new String(body);
//                    记录
                    log.debug("从RabbitMQ接收的消息：" + message);

//                    消息接受完，就要向ref集合中保存数据
                    MessageRefEntity entity = new MessageRefEntity();
//                    进行封装数据
                    entity.setMessageId(messageId);
//                     receiverId就是topic的名字
                    entity.setReceiverId(Integer.parseInt(topic));
                    entity.setReadFlag(false);
                    entity.setLastFlag(true);
//                    进行保存
                    messageService.insertRef(entity);
//                    向rabbitmq发送Ack应答
                    long deliveryTag = response.getEnvelope().getDeliveryTag();
                    channel.basicAck(deliveryTag, false);
                    i++;//累加接收消息的个数
                }
//                当消息队列没有消息了，就break退出循环即可
                else {
                    break;
                }
            }
        } catch (Exception e) {
            log.error("执行异常", e);
            throw new EmosException("接收消息失败");
        }
        return i;
    }

    @Async
    public int receiveAsync(String topic) {
        return receive(topic);
    }

//    删除消息队列
    public void deleteQueue(String topic){
        try (Connection connection = factory.newConnection();
             Channel channel = connection.createChannel();
        ) {
            channel.queueDelete(topic);
            log.debug("消息队列成功删除");
        }catch (Exception e) {
            log.error("删除队列失败", e);
            throw new EmosException("删除队列失败");
        }
    }

    @Async
    public void deleteQueueAsync(String topic){
        deleteQueue(topic);
    }

}
