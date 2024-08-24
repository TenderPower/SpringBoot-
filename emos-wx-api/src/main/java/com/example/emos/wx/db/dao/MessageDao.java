package com.example.emos.wx.db.dao;

import cn.hutool.core.date.DateField;
import cn.hutool.core.date.DateUtil;
import cn.hutool.json.JSONObject;
import com.example.emos.wx.db.pojo.MessageEntity;
import com.example.emos.wx.db.pojo.MessageRefEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Repository;

import java.util.Date;
import java.util.HashMap;
import java.util.List;

@Repository
public class MessageDao {
    @Autowired
    private MongoTemplate mongoTemplate;//声明变量

//    把数据存放到mogodb里面
    public String insert(MessageEntity entity){
//        PS：需要将Entity中的北京时间 转换为格林尼治时间
        Date sendTime=entity.getSendTime();
        sendTime=DateUtil.offset(sendTime, DateField.HOUR,8);
        entity.setSendTime(sendTime);
//        将数据进行保存
        entity=mongoTemplate.save(entity);

//        将mogo保存返回的结果中提取主键值
        return entity.get_id();
    }

//    利用分页进行查询数据
    public List<HashMap> searchMessageByPage(int userId,long start,int length){
//        定义JSON对象
        JSONObject json=new JSONObject();
//        将_id 进行数据类型转换 String
        json.set("$toString","$_id");
//        构造集合链接的前提设置 （初始化）
        Aggregation aggregation=Aggregation.newAggregation(
//                定义变量id
                Aggregation.addFields().addField("id").withValue(json).build(),
//                构造是那两个集合进行链接 以及用集合中的那个字段进行链接
                Aggregation.lookup("message_ref","id","messageId","ref"),
//                构造where条件
                Aggregation.match(Criteria.where("ref.receiverId").is(userId)),
//                对数据进行排序--降序
                Aggregation.sort(Sort.by(Sort.Direction.DESC,"sendTime")),
//                进行分页
                Aggregation.skip(start),
//                进行偏移量
                Aggregation.limit(length)
        );
//        集合链接
        AggregationResults<HashMap> results=mongoTemplate.aggregate(aggregation,"message",HashMap.class);
//        将结果提成List
        List<HashMap> list=results.getMappedResults();
//        先对提取的list中的数据进行预处理
        list.forEach(one->{
            List<MessageRefEntity> refList= (List<MessageRefEntity>) one.get("ref");
//            取出其中一个记录
            MessageRefEntity entity=refList.get(0);
//            从记录中提取所需要的字段
            boolean readFlag=entity.getReadFlag();
            String refId=entity.get_id();
//            one 就是 HashMap
            one.put("readFlag",readFlag);
            one.put("refId",refId);
//            我只需保存ref一部分信息即可，剩下的删除就行
            one.remove("ref");

            one.remove("_id");
//            进行时间的转换
            Date sendTime= (Date) one.get("sendTime");
            sendTime=DateUtil.offset(sendTime,DateField.HOUR,-8);

//            根据当天的时间 判断是否显示消息的年份
            String today=DateUtil.today();
            if(today.equals(DateUtil.date(sendTime).toDateStr())){
                one.put("sendTime",DateUtil.format(sendTime,"HH:mm"));
            }
            else{
                one.put("sendTime",DateUtil.format(sendTime,"yyyy/MM/dd"));
            }
        });
        return list;
    }

    public HashMap searchMessageById(String id){
        HashMap map=mongoTemplate.findById(id,HashMap.class,"message");
        Date sendTime= (Date) map.get("sendTime");
//        时间格式变换
        sendTime=DateUtil.offset(sendTime,DateField.HOUR,-8);
        map.replace("sendTime",DateUtil.format(sendTime,"yyyy-MM-dd HH:mm"));
        return map;
    }
}
